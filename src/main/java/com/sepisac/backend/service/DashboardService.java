package com.sepisac.backend.service;

import com.sepisac.backend.dto.*;
import com.sepisac.backend.model.*;
import com.sepisac.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final QuotationRepository quotationRepository;
    private final ProjectRepository projectRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoicePaymentRepository invoicePaymentRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderDetailRepository purchaseOrderDetailRepository;
    private final AuditLogRepository auditLogRepository;

    @Autowired
    public DashboardService(
            QuotationRepository quotationRepository,
            ProjectRepository projectRepository,
            InvoiceRepository invoiceRepository,
            InvoicePaymentRepository invoicePaymentRepository,
            InventoryItemRepository inventoryItemRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            PurchaseOrderDetailRepository purchaseOrderDetailRepository,
            AuditLogRepository auditLogRepository) {
        this.quotationRepository = quotationRepository;
        this.projectRepository = projectRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoicePaymentRepository = invoicePaymentRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.purchaseOrderDetailRepository = purchaseOrderDetailRepository;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * RN-D01 to RN-D07: KPI del Ciclo Comercial
     */
    public CommercialCycleKpiResponseDTO getCommercialCycleKpi(UUID companyId, LocalDate from, LocalDate to) {
        if (companyId == null) {
            throw new IllegalArgumentException("VALIDATION_ERROR: companyId es requerido");
        }
        if (from == null || to == null) {
            throw new IllegalArgumentException("VALIDATION_ERROR: Las fechas 'from' y 'to' son obligatorias");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("VALIDATION_ERROR: 'from' date must be before or equal to 'to' date");
        }
        if (ChronoUnit.DAYS.between(from, to) > 366) {
            throw new IllegalArgumentException("VALIDATION_ERROR: Date range cannot exceed 366 days");
        }

        OffsetDateTime fromOffset = from.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime toOffset = to.atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC);

        // RN-D02: Base quotations with status APROBADA, is_deleted = false, in date range
        List<QuotationEntity> quotations = quotationRepository.findApprovedQuotationsInRange(companyId, fromOffset, toOffset);
        if (quotations == null) {
            quotations = Collections.emptyList();
        }

        List<Long> creationToApprovalDays = new ArrayList<>();
        List<Long> approvalToProjectStartDays = new ArrayList<>();
        List<Long> projectStartToInvoiceDays = new ArrayList<>();
        List<Long> totalDays = new ArrayList<>();

        for (QuotationEntity quotation : quotations) {
            if (quotation == null) continue;

            // RN-D03: Approval date = createdAt of most recent audit_logs with action = APPROVE, module_affected = QUOTATIONS
            OffsetDateTime approvalDate = resolveApprovalDate(companyId, quotation.getId());

            // RN-D04: Project start_date and Invoice issue_date
            LocalDate projectStartDate = resolveProjectStartDate(companyId, quotation.getId());
            LocalDate invoiceIssueDate = resolveInvoiceIssueDate(companyId, quotation.getId());

            LocalDate quotationCreatedDate = quotation.getCreatedAt() != null
                    ? quotation.getCreatedAt().toLocalDate()
                    : null;

            // Stage 1: CREATION_TO_APPROVAL
            if (quotationCreatedDate != null && approvalDate != null) {
                long days = ChronoUnit.DAYS.between(quotationCreatedDate, approvalDate.toLocalDate());
                if (days >= 0) { // RN-D06: exclude negative differences
                    creationToApprovalDays.add(days);
                }
            }

            // Stage 2: APPROVAL_TO_PROJECT_START
            if (approvalDate != null && projectStartDate != null) {
                long days = ChronoUnit.DAYS.between(approvalDate.toLocalDate(), projectStartDate);
                if (days >= 0) {
                    approvalToProjectStartDays.add(days);
                }
            }

            // Stage 3: PROJECT_START_TO_INVOICE
            if (projectStartDate != null && invoiceIssueDate != null) {
                long days = ChronoUnit.DAYS.between(projectStartDate, invoiceIssueDate);
                if (days >= 0) {
                    projectStartToInvoiceDays.add(days);
                }
            }

            // Total: CREATION to INVOICE
            if (quotationCreatedDate != null && invoiceIssueDate != null) {
                long days = ChronoUnit.DAYS.between(quotationCreatedDate, invoiceIssueDate);
                if (days >= 0) {
                    totalDays.add(days);
                }
            }
        }

        StageKpiDTO creationToApproval = calculateStageKpi(creationToApprovalDays);
        StageKpiDTO approvalToProjectStart = calculateStageKpi(approvalToProjectStartDays);
        StageKpiDTO projectStartToInvoice = calculateStageKpi(projectStartToInvoiceDays);
        TotalKpiDTO total = calculateTotalKpi(totalDays);

        return new CommercialCycleKpiResponseDTO(
                creationToApproval,
                approvalToProjectStart,
                projectStartToInvoice,
                total
        );
    }

    /**
     * RN-D08: KPI de FacturaciÃ³n / Ingresos
     */
    public RevenueKpiResponseDTO getRevenueKpi(UUID companyId, LocalDate from, LocalDate to, String groupBy) {
        if (companyId == null) {
            throw new IllegalArgumentException("VALIDATION_ERROR: companyId es requerido");
        }
        if (from == null || to == null) {
            throw new IllegalArgumentException("VALIDATION_ERROR: Las fechas 'from' y 'to' son obligatorias");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("VALIDATION_ERROR: 'from' date must be before or equal to 'to' date");
        }
        if (ChronoUnit.DAYS.between(from, to) > 366) {
            throw new IllegalArgumentException("VALIDATION_ERROR: Date range cannot exceed 366 days");
        }

        if (groupBy == null || groupBy.isBlank()) {
            groupBy = "month";
        }
        String normalizedGroupBy = groupBy.trim().toLowerCase();
        if (!Set.of("day", "month", "year").contains(normalizedGroupBy)) {
            throw new IllegalArgumentException("VALIDATION_ERROR: groupBy debe ser 'day', 'month' o 'year'");
        }

        OffsetDateTime fromOffset = from.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime toOffset = to.atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC);

        List<InvoicePaymentEntity> payments = invoicePaymentRepository.findPaymentsByCompanyIdAndDateRange(
                companyId, fromOffset, toOffset
        );

        if (payments == null) {
            payments = Collections.emptyList();
        }

        DateTimeFormatter formatter = switch (normalizedGroupBy) {
            case "day" -> DateTimeFormatter.ofPattern("yyyy-MM-dd");
            case "year" -> DateTimeFormatter.ofPattern("yyyy");
            default -> DateTimeFormatter.ofPattern("yyyy-MM");
        };

        // Key: period -> Map<currency, {sum, count}>
        Map<String, Map<String, BigDecimal>> periodCurrencySums = new TreeMap<>();
        Map<String, Map<String, Integer>> periodCurrencyCounts = new HashMap<>();

        // Overall totals per currency: currency -> {sum, count}
        Map<String, BigDecimal> totalSumsByCurrency = new TreeMap<>();
        Map<String, Integer> totalCountsByCurrency = new HashMap<>();

        for (InvoicePaymentEntity payment : payments) {
            if (payment == null || payment.getPaymentDate() == null) continue;

            String period = payment.getPaymentDate().format(formatter);
            String currency = (payment.getInvoice() != null && payment.getInvoice().getCurrency() != null)
                    ? payment.getInvoice().getCurrency()
                    : "PEN";
            BigDecimal amount = payment.getAmountPaid() != null ? payment.getAmountPaid() : BigDecimal.ZERO;

            periodCurrencySums
                    .computeIfAbsent(period, k -> new TreeMap<>())
                    .merge(currency, amount, BigDecimal::add);

            periodCurrencyCounts
                    .computeIfAbsent(period, k -> new HashMap<>())
                    .merge(currency, 1, Integer::sum);

            totalSumsByCurrency.merge(currency, amount, BigDecimal::add);
            totalCountsByCurrency.merge(currency, 1, Integer::sum);
        }

        List<RevenueSeriesDTO> seriesList = new ArrayList<>();
        for (Map.Entry<String, Map<String, BigDecimal>> periodEntry : periodCurrencySums.entrySet()) {
            String period = periodEntry.getKey();
            for (Map.Entry<String, BigDecimal> currencyEntry : periodEntry.getValue().entrySet()) {
                String currency = currencyEntry.getKey();
                BigDecimal amount = currencyEntry.getValue().setScale(2, RoundingMode.HALF_UP);
                int count = periodCurrencyCounts.getOrDefault(period, Collections.emptyMap()).getOrDefault(currency, 0);
                seriesList.add(new RevenueSeriesDTO(period, currency, amount, count));
            }
        }

        List<CurrencyTotalDTO> totalsList = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : totalSumsByCurrency.entrySet()) {
            String currency = entry.getKey();
            BigDecimal amount = entry.getValue().setScale(2, RoundingMode.HALF_UP);
            int count = totalCountsByCurrency.getOrDefault(currency, 0);
            totalsList.add(new CurrencyTotalDTO(currency, amount, count));
        }

        return new RevenueKpiResponseDTO(normalizedGroupBy, seriesList, totalsList);
    }

    /**
     * RN-D09: KPI de Cuentas por Cobrar (Receivables)
     */
    public ReceivablesKpiResponseDTO getReceivablesKpi(UUID companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("VALIDATION_ERROR: companyId es requerido");
        }

        List<InvoiceEntity> invoices = invoiceRepository.findByCompanyId(companyId);
        if (invoices == null) {
            invoices = Collections.emptyList();
        }

        LocalDate today = LocalDate.now();

        // Status -> Currency -> {sumBalance, count}
        Map<String, Map<String, BigDecimal>> statusCurrencyBalance = new TreeMap<>();
        Map<String, Map<String, Integer>> statusCurrencyCount = new HashMap<>();

        int paidInvoiceCount = 0;
        Map<String, BigDecimal> paidSumsByCurrency = new TreeMap<>();
        Map<String, Integer> paidCountsByCurrency = new HashMap<>();

        for (InvoiceEntity invoice : invoices) {
            if (invoice == null || Boolean.TRUE.equals(invoice.getIsDeleted())) continue;

            BigDecimal totalAmount = invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO;
            String currency = invoice.getCurrency() != null ? invoice.getCurrency() : "PEN";

            BigDecimal paid = invoicePaymentRepository.sumAmountPaidByInvoiceId(invoice.getId());
            if (paid == null) {
                paid = BigDecimal.ZERO;
            }

            BigDecimal balance = totalAmount.subtract(paid);

            String effectiveStatus;
            if (balance.compareTo(BigDecimal.ZERO) <= 0) {
                effectiveStatus = "PAGADA";
            } else if (invoice.getDueDate() != null && invoice.getDueDate().isBefore(today)) {
                effectiveStatus = "VENCIDA";
            } else {
                effectiveStatus = (invoice.getPaymentStatus() != null && !invoice.getPaymentStatus().isBlank())
                        ? invoice.getPaymentStatus()
                        : "PENDIENTE";
            }

            if ("PAGADA".equals(effectiveStatus)) {
                paidInvoiceCount++;
                paidSumsByCurrency.merge(currency, paid, BigDecimal::add);
                paidCountsByCurrency.merge(currency, 1, Integer::sum);
            } else {
                statusCurrencyBalance
                        .computeIfAbsent(effectiveStatus, k -> new TreeMap<>())
                        .merge(currency, balance, BigDecimal::add);

                statusCurrencyCount
                        .computeIfAbsent(effectiveStatus, k -> new HashMap<>())
                        .merge(currency, 1, Integer::sum);
            }
        }

        List<ReceivableStatusGroupDTO> statusGroups = new ArrayList<>();
        for (Map.Entry<String, Map<String, BigDecimal>> statusEntry : statusCurrencyBalance.entrySet()) {
            String status = statusEntry.getKey();
            for (Map.Entry<String, BigDecimal> currEntry : statusEntry.getValue().entrySet()) {
                String currency = currEntry.getKey();
                BigDecimal balance = currEntry.getValue().setScale(2, RoundingMode.HALF_UP);
                int count = statusCurrencyCount.getOrDefault(status, Collections.emptyMap()).getOrDefault(currency, 0);
                statusGroups.add(new ReceivableStatusGroupDTO(status, currency, balance, count));
            }
        }

        List<CurrencyTotalDTO> paidTotalsList = new ArrayList<>();
        BigDecimal totalPaidAll = BigDecimal.ZERO;
        String primaryCurrency = "PEN";
        for (Map.Entry<String, BigDecimal> entry : paidSumsByCurrency.entrySet()) {
            String currency = entry.getKey();
            BigDecimal amount = entry.getValue().setScale(2, RoundingMode.HALF_UP);
            int count = paidCountsByCurrency.getOrDefault(currency, 0);
            paidTotalsList.add(new CurrencyTotalDTO(currency, amount, count));
            totalPaidAll = totalPaidAll.add(amount);
            primaryCurrency = currency;
        }

        PaidSummaryDTO paidSummary = new PaidSummaryDTO(
                paidInvoiceCount,
                paidTotalsList,
                totalPaidAll.setScale(2, RoundingMode.HALF_UP),
                primaryCurrency
        );

        return new ReceivablesKpiResponseDTO(statusGroups, paidSummary);
    }

    /**
     * RN-D10: KPI de Alertas de Inventario
     */
    public PageResponseDTO<InventoryAlertResponseDTO> getInventoryAlerts(UUID companyId, Pageable pageable) {
        if (companyId == null) {
            throw new IllegalArgumentException("VALIDATION_ERROR: companyId es requerido");
        }

        List<InventoryItemEntity> criticalItems = inventoryItemRepository.findCriticalItemsByCompanyId(companyId);
        if (criticalItems == null || criticalItems.isEmpty()) {
            return new PageResponseDTO<>(Collections.emptyList(), 0, pageable != null ? pageable.getPageSize() : 20, 0, 0, true, true);
        }

        List<UUID> itemIds = criticalItems.stream()
                .map(InventoryItemEntity::getId)
                .toList();

        List<PurchaseOrderEntity> openOrders = purchaseOrderRepository.findOpenPurchaseOrdersByItemIds(companyId, itemIds);
        if (openOrders == null) {
            openOrders = Collections.emptyList();
        }

        // Map: itemId -> List<OpenPurchaseOrderDTO>
        Map<UUID, List<OpenPurchaseOrderDTO>> itemOpenOrdersMap = new HashMap<>();

        for (PurchaseOrderEntity po : openOrders) {
            if (po == null || po.getId() == null) continue;

            List<PurchaseOrderDetailEntity> details = purchaseOrderDetailRepository.findByPurchaseOrderId(po.getId());
            if (details == null) continue;

            String supplierName = (po.getSupplier() != null && po.getSupplier().getBusinessName() != null)
                    ? po.getSupplier().getBusinessName()
                    : null;

            for (PurchaseOrderDetailEntity detail : details) {
                if (detail != null && detail.getInventoryItem() != null && detail.getInventoryItem().getId() != null) {
                    UUID itemId = detail.getInventoryItem().getId();
                    OpenPurchaseOrderDTO openOrderDTO = new OpenPurchaseOrderDTO(
                            po.getId(),
                            po.getOrderNumber(),
                            po.getStatus(),
                            detail.getQuantity() != null ? detail.getQuantity() : 0,
                            po.getCreatedAt(),
                            supplierName
                    );
                    itemOpenOrdersMap.computeIfAbsent(itemId, k -> new ArrayList<>()).add(openOrderDTO);
                }
            }
        }

        List<InventoryAlertResponseDTO> alerts = new ArrayList<>();
        for (InventoryItemEntity item : criticalItems) {
            if (item == null || Boolean.TRUE.equals(item.getIsDeleted())) continue;

            int stock = item.getStockQuantity() != null ? item.getStockQuantity() : 0;
            int minStock = item.getMinStockAlert() != null ? item.getMinStockAlert() : 0;
            int deficit = Math.max(0, minStock - stock);

            List<OpenPurchaseOrderDTO> openOrdersForItem = itemOpenOrdersMap.getOrDefault(item.getId(), Collections.emptyList());
            int totalRequested = openOrdersForItem.stream().mapToInt(OpenPurchaseOrderDTO::getRequestedQuantity).sum();
            boolean hasOpenOrder = !openOrdersForItem.isEmpty();

            alerts.add(new InventoryAlertResponseDTO(
                    item.getId(),
                    item.getSku(),
                    item.getName(),
                    stock,
                    minStock,
                    deficit,
                    openOrdersForItem,
                    totalRequested,
                    hasOpenOrder
            ));
        }

        int pageSize = pageable != null ? Math.max(1, pageable.getPageSize()) : 20;
        int pageNum = pageable != null ? Math.max(0, pageable.getPageNumber()) : 0;
        int totalElements = alerts.size();
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);

        int start = Math.min(pageNum * pageSize, totalElements);
        int end = Math.min(start + pageSize, totalElements);
        List<InventoryAlertResponseDTO> pageContent = alerts.subList(start, end);

        return new PageResponseDTO<>(
                pageContent,
                pageNum,
                pageSize,
                totalElements,
                totalPages,
                pageNum == 0,
                pageNum >= totalPages - 1 || totalPages == 0
        );
    }

    private OffsetDateTime resolveApprovalDate(UUID companyId, UUID quotationId) {
        Optional<AuditLogEntity> logOpt = auditLogRepository
                .findFirstByCompanyIdAndModuleAffectedAndEntityIdAndActionOrderByCreatedAtDesc(
                        companyId, "QUOTATIONS", quotationId, "APPROVE"
                );

        if (logOpt.isEmpty()) {
            logOpt = auditLogRepository
                    .findFirstByCompanyIdAndModuleAffectedAndEntityIdAndActionOrderByCreatedAtDesc(
                            companyId, "COMMERCIAL_QUOTATIONS", quotationId, "APPROVE"
                    );
        }

        if (logOpt.isEmpty()) {
            List<AuditLogEntity> logs = auditLogRepository.findByCompanyIdAndModuleAffectedAndEntityIdOrderByCreatedAtAsc(
                    companyId, "QUOTATIONS", quotationId
            );
            if (logs != null && !logs.isEmpty()) {
                logOpt = logs.stream()
                        .filter(l -> "APPROVE".equalsIgnoreCase(l.getAction()))
                        .reduce((first, second) -> second);
            }
        }

        return logOpt.map(AuditLogEntity::getCreatedAt).orElse(null);
    }

    private LocalDate resolveProjectStartDate(UUID companyId, UUID quotationId) {
        List<ProjectEntity> projects = projectRepository.findByQuotationId(quotationId);
        if (projects != null && !projects.isEmpty()) {
            return projects.stream()
                    .filter(p -> p != null && !Boolean.TRUE.equals(p.getIsDeleted()) && p.getStartDate() != null)
                    .map(ProjectEntity::getStartDate)
                    .min(LocalDate::compareTo)
                    .orElse(null);
        }

        List<ProjectEntity> companyProjects = projectRepository.findByCompanyId(companyId);
        if (companyProjects != null) {
            return companyProjects.stream()
                    .filter(p -> p != null && !Boolean.TRUE.equals(p.getIsDeleted())
                            && p.getQuotation() != null && quotationId.equals(p.getQuotation().getId())
                            && p.getStartDate() != null)
                    .map(ProjectEntity::getStartDate)
                    .min(LocalDate::compareTo)
                    .orElse(null);
        }
        return null;
    }

    private LocalDate resolveInvoiceIssueDate(UUID companyId, UUID quotationId) {
        List<InvoiceEntity> invoices = invoiceRepository.findByQuotationId(quotationId);
        if (invoices != null && !invoices.isEmpty()) {
            return invoices.stream()
                    .filter(i -> i != null && !Boolean.TRUE.equals(i.getIsDeleted()) && i.getIssueDate() != null)
                    .map(InvoiceEntity::getIssueDate)
                    .min(LocalDate::compareTo)
                    .orElse(null);
        }

        List<InvoiceEntity> companyInvoices = invoiceRepository.findByCompanyId(companyId);
        if (companyInvoices != null) {
            return companyInvoices.stream()
                    .filter(i -> i != null && !Boolean.TRUE.equals(i.getIsDeleted())
                            && i.getQuotation() != null && quotationId.equals(i.getQuotation().getId())
                            && i.getIssueDate() != null)
                    .map(InvoiceEntity::getIssueDate)
                    .min(LocalDate::compareTo)
                    .orElse(null);
        }
        return null;
    }

    private StageKpiDTO calculateStageKpi(List<Long> daysList) {
        if (daysList == null || daysList.isEmpty()) {
            return new StageKpiDTO(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), 0);
        }
        long sum = 0;
        for (Long d : daysList) {
            sum += d;
        }
        BigDecimal avg = BigDecimal.valueOf(sum)
                .divide(BigDecimal.valueOf(daysList.size()), 2, RoundingMode.HALF_UP);
        return new StageKpiDTO(avg, daysList.size());
    }

    private TotalKpiDTO calculateTotalKpi(List<Long> daysList) {
        if (daysList == null || daysList.isEmpty()) {
            return new TotalKpiDTO(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), 0);
        }
        long sum = 0;
        for (Long d : daysList) {
            sum += d;
        }
        BigDecimal avg = BigDecimal.valueOf(sum)
                .divide(BigDecimal.valueOf(daysList.size()), 2, RoundingMode.HALF_UP);
        return new TotalKpiDTO(avg, daysList.size());
    }

    public DashboardResponseDTO getUnifiedDashboardData(UUID companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("VALIDATION_ERROR: companyId es requerido");
        }

        DashboardResponseDTO response = new DashboardResponseDTO();
        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();
        int currentMonth = today.getMonthValue();

        LocalDate prevMonthDate = today.minusMonths(1);
        int prevYear = prevMonthDate.getYear();
        int prevMonth = prevMonthDate.getMonthValue();

        // 1. Ingresos Mensuales (Payments)
        List<InvoicePaymentEntity> payments = invoicePaymentRepository.findByCompanyId(companyId);
        if (payments == null) payments = Collections.emptyList();

        BigDecimal ingresosCurrent = BigDecimal.ZERO;
        BigDecimal ingresosPrev = BigDecimal.ZERO;

        for (InvoicePaymentEntity payment : payments) {
            if (payment == null || payment.getPaymentDate() == null || payment.getAmountPaid() == null) continue;
            LocalDate pDate = payment.getPaymentDate().toLocalDate();
            if (pDate.getYear() == currentYear && pDate.getMonthValue() == currentMonth) {
                ingresosCurrent = ingresosCurrent.add(payment.getAmountPaid());
            } else if (pDate.getYear() == prevYear && pDate.getMonthValue() == prevMonth) {
                ingresosPrev = ingresosPrev.add(payment.getAmountPaid());
            }
        }
        response.setIngresosMensuales(ingresosCurrent);

        BigDecimal ingresosTrend = BigDecimal.ZERO;
        if (ingresosPrev.compareTo(BigDecimal.ZERO) > 0) {
            ingresosTrend = ingresosCurrent.subtract(ingresosPrev)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(ingresosPrev, 1, RoundingMode.HALF_UP);
        } else if (ingresosCurrent.compareTo(BigDecimal.ZERO) > 0) {
            ingresosTrend = BigDecimal.valueOf(100.0);
        }
        response.setIngresosTrend(ingresosTrend);

        // 2. Egresos Mensuales (Purchase Orders)
        List<PurchaseOrderEntity> pos = purchaseOrderRepository.findByCompanyId(companyId);
        if (pos == null) pos = Collections.emptyList();

        BigDecimal egresosCurrent = BigDecimal.ZERO;
        BigDecimal egresosPrev = BigDecimal.ZERO;

        for (PurchaseOrderEntity po : pos) {
            if (po == null || po.getCreatedAt() == null || po.getTotalAmount() == null) continue;
            if ("CANCELADA".equalsIgnoreCase(po.getStatus())) continue;
            LocalDate poDate = po.getCreatedAt().toLocalDate();
            if (poDate.getYear() == currentYear && poDate.getMonthValue() == currentMonth) {
                egresosCurrent = egresosCurrent.add(po.getTotalAmount());
            } else if (poDate.getYear() == prevYear && poDate.getMonthValue() == prevMonth) {
                egresosPrev = egresosPrev.add(po.getTotalAmount());
            }
        }
        response.setEgresosMensuales(egresosCurrent);

        BigDecimal egresosTrend = BigDecimal.ZERO;
        if (egresosPrev.compareTo(BigDecimal.ZERO) > 0) {
            egresosTrend = egresosCurrent.subtract(egresosPrev)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(egresosPrev, 1, RoundingMode.HALF_UP);
        } else if (egresosCurrent.compareTo(BigDecimal.ZERO) > 0) {
            egresosTrend = BigDecimal.valueOf(100.0);
        }
        response.setEgresosTrend(egresosTrend);

        // 3. Cotizaciones Aprobadas
        List<QuotationEntity> quotations = quotationRepository.findByCompanyId(companyId);
        if (quotations == null) quotations = Collections.emptyList();

        int cotizacionesApprovedCurrent = 0;
        int cotizacionesApprovedPrev = 0;

        for (QuotationEntity q : quotations) {
            if (q == null || q.getStatus() == null) continue;
            if ("APROBADA".equalsIgnoreCase(q.getStatus())) {
                LocalDate qDate = q.getCreatedAt() != null ? q.getCreatedAt().toLocalDate() : today;
                if (qDate.getYear() == currentYear && qDate.getMonthValue() == currentMonth) {
                    cotizacionesApprovedCurrent++;
                } else if (qDate.getYear() == prevYear && qDate.getMonthValue() == prevMonth) {
                    cotizacionesApprovedPrev++;
                }
            }
        }
        response.setCotizacionesAprobadas(cotizacionesApprovedCurrent);
        response.setCotizacionesTrend(cotizacionesApprovedCurrent - cotizacionesApprovedPrev);

        // 4. Proyectos Activos y por Terminar
        List<ProjectEntity> projects = projectRepository.findByCompanyId(companyId);
        if (projects == null) projects = Collections.emptyList();

        int activos = 0;
        int terminanPronto = 0;
        LocalDate thresholdSoon = today.plusDays(30);

        int countEnProceso = 0;
        int countPendiente = 0;
        int countCompletado = 0;
        int countCancelado = 0;

        for (ProjectEntity p : projects) {
            if (p == null) continue;
            String st = p.getStatus() != null ? p.getStatus().toUpperCase() : "PENDIENTE";
            boolean isActivo = st.contains("PROCESO") || st.contains("PROGRESO");
            if (isActivo) {
                activos++;
                countEnProceso++;
                if (p.getEndDate() != null && !p.getEndDate().isBefore(today) && !p.getEndDate().isAfter(thresholdSoon)) {
                    terminanPronto++;
                }
            } else if (st.contains("COMPLET")) {
                countCompletado++;
            } else if (st.contains("CANCEL")) {
                countCancelado++;
            } else {
                countPendiente++;
            }
        }
        response.setProyectosActivos(activos);
        response.setProyectosTerminanPronto(terminanPronto);

        // 5. Estado de Proyectos (PieData)
        List<DashboardResponseDTO.PieData> pieDataList = new ArrayList<>();
        pieDataList.add(new DashboardResponseDTO.PieData("En Proceso", countEnProceso, "#0062ff"));
        pieDataList.add(new DashboardResponseDTO.PieData("Pendientes", countPendiente, "#eab308"));
        pieDataList.add(new DashboardResponseDTO.PieData("Completados", countCompletado, "#16a34a"));
        pieDataList.add(new DashboardResponseDTO.PieData("Cancelados", countCancelado, "#ef4444"));
        response.setEstadoProyectos(pieDataList);

        // 6. Ingresos vs Egresos (Ultimos 8 meses, en miles)
        String[] monthNames = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Set", "Oct", "Nov", "Dic"};
        List<DashboardResponseDTO.ChartData> chartDataList = new ArrayList<>();

        for (int i = 7; i >= 0; i--) {
            LocalDate mDate = today.minusMonths(i);
            int y = mDate.getYear();
            int m = mDate.getMonthValue();
            String name = monthNames[m - 1];

            BigDecimal mIngresos = BigDecimal.ZERO;
            for (InvoicePaymentEntity pay : payments) {
                if (pay.getPaymentDate() == null || pay.getAmountPaid() == null) continue;
                LocalDate d = pay.getPaymentDate().toLocalDate();
                if (d.getYear() == y && d.getMonthValue() == m) {
                    mIngresos = mIngresos.add(pay.getAmountPaid());
                }
            }

            BigDecimal mEgresos = BigDecimal.ZERO;
            for (PurchaseOrderEntity po : pos) {
                if (po.getCreatedAt() == null || po.getTotalAmount() == null) continue;
                if ("CANCELADA".equalsIgnoreCase(po.getStatus())) continue;
                LocalDate d = po.getCreatedAt().toLocalDate();
                if (d.getYear() == y && d.getMonthValue() == m) {
                    mEgresos = mEgresos.add(po.getTotalAmount());
                }
            }

            BigDecimal inThousands = mIngresos.divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);
            BigDecimal egThousands = mEgresos.divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);

            chartDataList.add(new DashboardResponseDTO.ChartData(name, inThousands, egThousands));
        }
        response.setIngresosVsEgresos(chartDataList);

        // 7. Proyectos Recientes (Top 5 ordenados por fecha de creacion desc)
        List<ProjectResponseDTO> recentProjects = projects.stream()
                .sorted((a, b) -> {
                    OffsetDateTime timeA = a.getCreatedAt() != null ? a.getCreatedAt() : OffsetDateTime.MIN;
                    OffsetDateTime timeB = b.getCreatedAt() != null ? b.getCreatedAt() : OffsetDateTime.MIN;
                    return timeB.compareTo(timeA);
                })
                .limit(5)
                .map(this::mapProjectToDTO)
                .collect(Collectors.toList());
        response.setProyectosRecientes(recentProjects);

        return response;
    }

    private ProjectResponseDTO mapProjectToDTO(ProjectEntity p) {
        ProjectResponseDTO dto = new ProjectResponseDTO();
        dto.setId(p.getId());
        dto.setCompanyId(p.getCompany() != null ? p.getCompany().getId() : null);
        dto.setQuotationId(p.getQuotation() != null ? p.getQuotation().getId() : null);
        dto.setCode(p.getCode());
        dto.setTitle(p.getTitle());
        dto.setDescription(p.getDescription());
        dto.setStatus(p.getStatus());
        dto.setStartDate(p.getStartDate());
        dto.setEndDate(p.getEndDate());
        dto.setClientName(p.getClientName());
        dto.setCreatedAt(p.getCreatedAt());
        dto.setUpdatedAt(p.getUpdatedAt());
        return dto;
    }
}
