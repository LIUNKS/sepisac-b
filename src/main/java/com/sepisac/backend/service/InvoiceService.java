package com.sepisac.backend.service;

import com.sepisac.backend.dto.InvoicePaymentCreateDTO;
import com.sepisac.backend.dto.InvoicePaymentResponseDTO;
import com.sepisac.backend.dto.InvoiceResponseDTO;
import com.sepisac.backend.exception.BusinessRuleException;
import com.sepisac.backend.exception.OverpaymentException;
import com.sepisac.backend.exception.ResourceNotFoundException;
import com.sepisac.backend.model.*;
import com.sepisac.backend.repository.*;
import com.sepisac.backend.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoicePaymentRepository invoicePaymentRepository;
    private final QuotationRepository quotationRepository;
    private final ProjectRepository projectRepository;
    private final AuditLogService auditLogService;

    @Autowired
    public InvoiceService(
            InvoiceRepository invoiceRepository,
            InvoicePaymentRepository invoicePaymentRepository,
            QuotationRepository quotationRepository,
            ProjectRepository projectRepository,
            AuditLogService auditLogService) {
        this.invoiceRepository = invoiceRepository;
        this.invoicePaymentRepository = invoicePaymentRepository;
        this.quotationRepository = quotationRepository;
        this.projectRepository = projectRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(rollbackFor = Exception.class)
    public InvoiceResponseDTO createInvoiceFromQuotation(UUID quotationId, UserPrincipal currentUser) {
        QuotationEntity quotation = quotationRepository.findById(quotationId)
                .orElseThrow(() -> new ResourceNotFoundException("Cotización no encontrada con ID: " + quotationId));

        if (!"APROBADA".equalsIgnoreCase(quotation.getStatus())) {
            throw new BusinessRuleException("Solo se pueden emitir facturas para cotizaciones aprobadas. Estado actual: " + quotation.getStatus());
        }

        enforceTenantAccess(quotation.getCompany().getId(), currentUser);

        String invoiceNumber = "FAC-" + quotation.getQuotationNumber();
        if (invoiceRepository.existsByCompanyIdAndInvoiceNumber(quotation.getCompany().getId(), invoiceNumber)) {
            invoiceNumber = invoiceNumber + "-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        }

        // Buscar si ya existe un proyecto asociado a esta cotización
        ProjectEntity associatedProject = projectRepository.findByCompanyId(quotation.getCompany().getId()).stream()
                .filter(p -> p.getQuotation() != null && quotationId.equals(p.getQuotation().getId()))
                .findFirst()
                .orElse(null);

        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setCompany(quotation.getCompany());
        invoice.setQuotation(quotation);
        invoice.setProject(associatedProject);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setCurrency(quotation.getCurrency() != null ? quotation.getCurrency() : "PEN");
        invoice.setTotalAmount(quotation.getTotalAmount() != null ? quotation.getTotalAmount() : BigDecimal.ZERO);
        invoice.setPaymentStatus("PENDIENTE");
        invoice.setIssueDate(LocalDate.now());
        invoice.setDueDate(LocalDate.now().plusDays(30));
        invoice.setIsDeleted(false);

        InvoiceEntity savedInvoice = invoiceRepository.save(invoice);

        if (currentUser != null) {
            auditLogService.log(
                    savedInvoice.getCompany().getId(),
                    currentUser.getId(),
                    "INVOICE_CREATED",
                    "FINANCE",
                    "Factura emitida exitosamente: " + savedInvoice.getInvoiceNumber() + " por monto " + savedInvoice.getTotalAmount()
            );
        }

        return mapToInvoiceDTO(savedInvoice, BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public InvoiceResponseDTO getInvoiceById(UUID id, UserPrincipal currentUser) {
        InvoiceEntity invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Factura no encontrada con ID: " + id));

        enforceTenantAccess(invoice.getCompany().getId(), currentUser);

        BigDecimal totalPaid = invoicePaymentRepository.sumAmountPaidByInvoiceId(id);
        return mapToInvoiceDTO(invoice, totalPaid != null ? totalPaid : BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponseDTO> getInvoicesByStatus(String status, UUID companyId, UserPrincipal currentUser) {
        UUID effectiveCompanyId = resolveCompanyId(companyId, currentUser);

        String normalizedStatus = status != null ? status.trim().toUpperCase() : "PENDIENTE";

        List<InvoiceEntity> invoices;
        if (effectiveCompanyId != null) {
            invoices = invoiceRepository.findByCompanyIdAndPaymentStatus(effectiveCompanyId, normalizedStatus);
        } else {
            invoices = invoiceRepository.findByPaymentStatus(normalizedStatus);
        }

        return invoices.stream()
                .map(inv -> {
                    BigDecimal paid = invoicePaymentRepository.sumAmountPaidByInvoiceId(inv.getId());
                    return mapToInvoiceDTO(inv, paid != null ? paid : BigDecimal.ZERO);
                })
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public InvoicePaymentResponseDTO registerPayment(UUID invoiceId, InvoicePaymentCreateDTO dto, UserPrincipal currentUser) {
        InvoiceEntity invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Factura no encontrada con ID: " + invoiceId));

        enforceTenantAccess(invoice.getCompany().getId(), currentUser);

        // Validación de moneda (Test 3: Integridad de divisas)
        BigDecimal effectiveAmount = dto.getAmountPaid();
        if (dto.getCurrency() != null && !dto.getCurrency().trim().isEmpty()) {
            String paymentCurrency = dto.getCurrency().trim().toUpperCase();
            String invoiceCurrency = invoice.getCurrency() != null ? invoice.getCurrency().toUpperCase() : "PEN";

            if (!paymentCurrency.equals(invoiceCurrency)) {
                if (dto.getExchangeRate() == null || dto.getExchangeRate().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Incompatibilidad de divisas: la factura está emitida en " + 
                            invoiceCurrency + " y el pago se envió en " + paymentCurrency + " sin una tasa de conversión válida.");
                }
                // Si la factura está en PEN y el pago en USD, convertir aplicando exchangeRate: USD * Tasa = PEN
                if ("USD".equals(paymentCurrency) && "PEN".equals(invoiceCurrency)) {
                    effectiveAmount = dto.getAmountPaid().multiply(dto.getExchangeRate()).setScale(2, RoundingMode.HALF_UP);
                } else if ("PEN".equals(paymentCurrency) && "USD".equals(invoiceCurrency)) {
                    effectiveAmount = dto.getAmountPaid().divide(dto.getExchangeRate(), 2, RoundingMode.HALF_UP);
                }
            }
        }

        // Consultar sumatoria actual a nivel de base de datos (Criterio 4.3)
        BigDecimal totalPaidBefore = invoicePaymentRepository.sumAmountPaidByInvoiceId(invoiceId);
        if (totalPaidBefore == null) {
            totalPaidBefore = BigDecimal.ZERO;
        }

        // Precisión financiera con BigDecimal (Criterio 4.1)
        BigDecimal balanceDue = invoice.getTotalAmount().subtract(totalPaidBefore);

        // Prevención de sobrepagos (Test 2: HTTP 422)
        if (effectiveAmount.compareTo(balanceDue) > 0) {
            throw new OverpaymentException("El abono de " + effectiveAmount + " " + invoice.getCurrency() + 
                    " excede el saldo deudor pendiente de " + balanceDue + " " + invoice.getCurrency() + 
                    " para la factura " + invoice.getInvoiceNumber());
        }

        // Registrar el pago en invoice_payments
        InvoicePaymentEntity payment = new InvoicePaymentEntity();
        payment.setInvoice(invoice);
        payment.setAmountPaid(effectiveAmount);
        payment.setPaymentMethod(dto.getPaymentMethod().toUpperCase());
        payment.setReferenceCode(dto.getReferenceCode());
        payment.setPaymentDate(dto.getPaymentDate() != null ? dto.getPaymentDate() : OffsetDateTime.now());

        InvoicePaymentEntity savedPayment = invoicePaymentRepository.save(payment);

        // Actualizar automáticamente el estado de la factura (Test 1)
        BigDecimal totalPaidAfter = totalPaidBefore.add(effectiveAmount);
        BigDecimal newBalance = invoice.getTotalAmount().subtract(totalPaidAfter);

        String resultingStatus;
        if (newBalance.compareTo(BigDecimal.ZERO) <= 0) {
            resultingStatus = "PAGADA";
        } else {
            resultingStatus = "PARCIAL";
        }

        invoice.setPaymentStatus(resultingStatus);
        invoiceRepository.save(invoice);

        // Auditoría RLS (Sección 1.3)
        if (currentUser != null) {
            auditLogService.log(
                    invoice.getCompany().getId(),
                    currentUser.getId(),
                    "PAYMENT_RECEIVED",
                    "FINANCE",
                    "Abono de " + effectiveAmount + " " + invoice.getCurrency() + " registrado para factura " + 
                            invoice.getInvoiceNumber() + ". Estado resultante: " + resultingStatus
            );
        }

        return new InvoicePaymentResponseDTO(
                savedPayment.getId(),
                invoice.getId(),
                invoice.getInvoiceNumber(),
                savedPayment.getAmountPaid(),
                savedPayment.getPaymentMethod(),
                savedPayment.getReferenceCode(),
                savedPayment.getPaymentDate(),
                resultingStatus,
                newBalance.max(BigDecimal.ZERO)
        );
    }

    @Transactional(readOnly = true)
    public List<InvoicePaymentResponseDTO> getPaymentsByInvoice(UUID invoiceId, UserPrincipal currentUser) {
        InvoiceEntity invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Factura no encontrada con ID: " + invoiceId));

        enforceTenantAccess(invoice.getCompany().getId(), currentUser);

        return invoicePaymentRepository.findByInvoiceId(invoiceId).stream()
                .map(p -> new InvoicePaymentResponseDTO(
                        p.getId(),
                        invoice.getId(),
                        invoice.getInvoiceNumber(),
                        p.getAmountPaid(),
                        p.getPaymentMethod(),
                        p.getReferenceCode(),
                        p.getPaymentDate(),
                        invoice.getPaymentStatus(),
                        BigDecimal.ZERO
                ))
                .collect(Collectors.toList());
    }

    private void enforceTenantAccess(UUID resourceCompanyId, UserPrincipal currentUser) {
        if (currentUser == null) return;
        boolean isSuperAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> "ROLE_SUPERADMIN".equals(a.getAuthority()));
        if (!isSuperAdmin && !resourceCompanyId.equals(currentUser.getCompanyId())) {
            throw new ResourceNotFoundException("Factura no encontrada");
        }
    }

    private UUID resolveCompanyId(UUID companyIdParam, UserPrincipal currentUser) {
        if (currentUser == null) return companyIdParam;
        boolean isSuperAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> "ROLE_SUPERADMIN".equals(a.getAuthority()));
        if (isSuperAdmin) {
            return companyIdParam;
        }
        return currentUser.getCompanyId();
    }

    private InvoiceResponseDTO mapToInvoiceDTO(InvoiceEntity invoice, BigDecimal totalPaid) {
        BigDecimal totalAmount = invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal paid = totalPaid != null ? totalPaid : BigDecimal.ZERO;
        BigDecimal balance = totalAmount.subtract(paid).max(BigDecimal.ZERO);

        return new InvoiceResponseDTO(
                invoice.getId(),
                invoice.getCompany().getId(),
                invoice.getCompany().getBusinessName(),
                invoice.getQuotation() != null ? invoice.getQuotation().getId() : null,
                invoice.getQuotation() != null ? invoice.getQuotation().getQuotationNumber() : null,
                invoice.getProject() != null ? invoice.getProject().getId() : null,
                invoice.getProject() != null ? invoice.getProject().getCode() : null,
                invoice.getQuotation() != null ? invoice.getQuotation().getClientName() : null,
                invoice.getInvoiceNumber(),
                invoice.getCurrency(),
                totalAmount,
                paid,
                balance,
                invoice.getPaymentStatus(),
                invoice.getIssueDate(),
                invoice.getDueDate(),
                invoice.getCreatedAt()
        );
    }
}
