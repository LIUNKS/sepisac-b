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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Sprint 5: Módulo Financiero y Facturación - Casos Críticos de Negocio")
class InvoiceSprint5CriticalTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private InvoicePaymentRepository invoicePaymentRepository;

    @Mock
    private QuotationRepository quotationRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private InvoiceService invoiceService;

    private CompanyEntity companyTenantA;
    private CompanyEntity companyTenantB;
    private QuotationEntity testQuotation;
    private InvoiceEntity testInvoice;
    private UserPrincipal userTenantA;
    private UserPrincipal userTenantB;

    private UUID tenantAId;
    private UUID tenantBId;
    private UUID invoiceId;
    private UUID quotationId;
    private UUID userIdA;

    @BeforeEach
    void setUp() {
        tenantAId = UUID.randomUUID();
        tenantBId = UUID.randomUUID();
        invoiceId = UUID.randomUUID();
        quotationId = UUID.randomUUID();
        userIdA = UUID.randomUUID();

        companyTenantA = new CompanyEntity();
        companyTenantA.setId(tenantAId);
        companyTenantA.setBusinessName("Constructora Tenant A S.A.C.");

        companyTenantB = new CompanyEntity();
        companyTenantB.setId(tenantBId);
        companyTenantB.setBusinessName("Servicios Tenant B S.A.C.");

        testQuotation = new QuotationEntity();
        testQuotation.setId(quotationId);
        testQuotation.setCompany(companyTenantA);
        testQuotation.setQuotationNumber("COT-2026-099");
        testQuotation.setClientName("Minera Cerro Verde");
        testQuotation.setServiceType("Mantenimiento de Motores");
        testQuotation.setCurrency("PEN");
        testQuotation.setTotalAmount(new BigDecimal("1000.00"));
        testQuotation.setStatus("APROBADA");

        testInvoice = new InvoiceEntity();
        testInvoice.setId(invoiceId);
        testInvoice.setCompany(companyTenantA);
        testInvoice.setQuotation(testQuotation);
        testInvoice.setInvoiceNumber("FAC-2026-001");
        testInvoice.setCurrency("PEN");
        testInvoice.setTotalAmount(new BigDecimal("1000.00"));
        testInvoice.setPaymentStatus("PENDIENTE");
        testInvoice.setIssueDate(LocalDate.now());
        testInvoice.setDueDate(LocalDate.now().plusDays(30));

        userTenantA = new UserPrincipal(
                userIdA, "admin@tenanta.com", "adminA", "secret",
                tenantAId, List.of(new SimpleGrantedAuthority("ROLE_ADMIN_EMPRESA")), true
        );

        userTenantB = new UserPrincipal(
                UUID.randomUUID(), "admin@tenantb.com", "adminB", "secret",
                tenantBId, List.of(new SimpleGrantedAuthority("ROLE_ADMIN_EMPRESA")), true
        );
    }

    @Nested
    @DisplayName("Casos Críticos de la Especificación Técnica")
    class CriticalSprint5SpecificationTests {

        @Test
        @DisplayName("Test 1: Transición Automática de Estados de Pago (PENDIENTE -> PARCIAL -> PAGADA)")
        void test1_automaticPaymentStatusTransition_partialThenPaid() {
            // Escenario: Factura por S/. 1,000.
            // Primer pago de S/. 400 -> estado debe cambiar a PARCIAL.
            when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(testInvoice));
            // Antes del primer pago, total abonado es 0
            when(invoicePaymentRepository.sumAmountPaidByInvoiceId(invoiceId)).thenReturn(BigDecimal.ZERO);
            when(invoicePaymentRepository.save(any(InvoicePaymentEntity.class))).thenAnswer(i -> {
                InvoicePaymentEntity p = i.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });
            when(invoiceRepository.save(any(InvoiceEntity.class))).thenAnswer(i -> i.getArgument(0));

            InvoicePaymentCreateDTO payment1DTO = new InvoicePaymentCreateDTO(
                    new BigDecimal("400.00"), "TRANSFERENCIA", "OP-1001", "PEN", null);

            InvoicePaymentResponseDTO response1 = invoiceService.registerPayment(invoiceId, payment1DTO, userTenantA);

            assertThat(response1.getResultingPaymentStatus()).isEqualTo("PARCIAL");
            assertThat(response1.getRemainingBalance()).isEqualByComparingTo(new BigDecimal("600.00"));
            assertThat(testInvoice.getPaymentStatus()).isEqualTo("PARCIAL");

            // Segundo pago de S/. 600 -> estado debe cambiar a PAGADA.
            // La BD sumará ahora los 400 acumulados previos
            when(invoicePaymentRepository.sumAmountPaidByInvoiceId(invoiceId)).thenReturn(new BigDecimal("400.00"));

            InvoicePaymentCreateDTO payment2DTO = new InvoicePaymentCreateDTO(
                    new BigDecimal("600.00"), "DEPOSITO", "OP-1002", "PEN", null);

            InvoicePaymentResponseDTO response2 = invoiceService.registerPayment(invoiceId, payment2DTO, userTenantA);

            assertThat(response2.getResultingPaymentStatus()).isEqualTo("PAGADA");
            assertThat(response2.getRemainingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(testInvoice.getPaymentStatus()).isEqualTo("PAGADA");

            verify(auditLogService, times(2)).log(eq(tenantAId), eq(userIdA), eq("PAYMENT_RECEIVED"), eq("FINANCE"), anyString());
        }

        @Test
        @DisplayName("Test 2: Prevención de Sobrepagos (Rechazar abono mayor al saldo pendiente con HTTP 422)")
        void test2_overpaymentPrevention_throwsOverpaymentException422() {
            // Escenario: Factura de S/. 1,000 con S/. 800 ya pagados (saldo pendiente: S/. 200).
            // Intento de pago por S/. 500 debe ser rechazado con OverpaymentException.
            when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(testInvoice));
            when(invoicePaymentRepository.sumAmountPaidByInvoiceId(invoiceId)).thenReturn(new BigDecimal("800.00"));

            InvoicePaymentCreateDTO overpaymentDTO = new InvoicePaymentCreateDTO(
                    new BigDecimal("500.00"), "EFECTIVO", "OP-OVERPAY", "PEN", null);

            assertThatThrownBy(() -> invoiceService.registerPayment(invoiceId, overpaymentDTO, userTenantA))
                    .isInstanceOf(OverpaymentException.class)
                    .hasMessageContaining("excede el saldo deudor pendiente");

            verify(invoicePaymentRepository, never()).save(any());
            verify(auditLogService, never()).log(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Test 3: Integridad de Divisas (Pagar en USD una factura en PEN sin tipo de cambio arroja HTTP 400)")
        void test3_currencyIntegrity_rejectsMismatchWithoutExchangeRate() {
            // Escenario: Factura emitida en PEN. Intento de pago en USD sin tasa de conversión.
            when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(testInvoice));

            InvoicePaymentCreateDTO mismatchDTO = new InvoicePaymentCreateDTO(
                    new BigDecimal("200.00"), "TRANSFERENCIA", "OP-USD-01", "USD", null);

            assertThatThrownBy(() -> invoiceService.registerPayment(invoiceId, mismatchDTO, userTenantA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Incompatibilidad de divisas");

            verify(invoicePaymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Test 4: Aislamiento Multi-Tenant RLS (Usuario Tenant B no puede acceder a factura Tenant A)")
        void test4_multiTenantIsolation_returnsNotFoundForOtherTenant() {
            // Escenario: Usuario del Tenant B intenta consultar una factura perteneciente al Tenant A.
            when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(testInvoice));

            assertThatThrownBy(() -> invoiceService.getInvoiceById(invoiceId, userTenantB))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Factura no encontrada");
        }
    }

    @Nested
    @DisplayName("Emisión de Facturas y Consultas por Estado")
    class InvoiceCreationAndQueryTests {

        @Test
        @DisplayName("Debe crear factura exitosamente desde cotización APROBADA")
        void shouldCreateInvoiceFromApprovedQuotation() {
            when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(testQuotation));
            when(invoiceRepository.existsByCompanyIdAndInvoiceNumber(any(), any())).thenReturn(false);
            when(invoiceRepository.save(any(InvoiceEntity.class))).thenAnswer(i -> {
                InvoiceEntity inv = i.getArgument(0);
                inv.setId(invoiceId);
                return inv;
            });

            InvoiceResponseDTO response = invoiceService.createInvoiceFromQuotation(quotationId, userTenantA);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(invoiceId);
            assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
            assertThat(response.getCurrency()).isEqualTo("PEN");
            assertThat(response.getPaymentStatus()).isEqualTo("PENDIENTE");
            assertThat(response.getClientName()).isEqualTo("Minera Cerro Verde");

            verify(invoiceRepository).save(any(InvoiceEntity.class));
        }

        @Test
        @DisplayName("Debe fallar si la cotización no está en estado APROBADA")
        void shouldFailWhenQuotationNotApproved() {
            testQuotation.setStatus("BORRADOR");
            when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(testQuotation));

            assertThatThrownBy(() -> invoiceService.createInvoiceFromQuotation(quotationId, userTenantA))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Solo se pueden emitir facturas para cotizaciones aprobadas");

            verify(invoiceRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe listar facturas filtrando por estado de cobranza")
        void shouldListInvoicesFilteredByStatus() {
            when(invoiceRepository.findByCompanyIdAndPaymentStatus(tenantAId, "PENDIENTE"))
                    .thenReturn(List.of(testInvoice));
            when(invoicePaymentRepository.sumAmountPaidByInvoiceId(invoiceId)).thenReturn(BigDecimal.ZERO);

            List<InvoiceResponseDTO> list = invoiceService.getInvoicesByStatus("PENDIENTE", tenantAId, userTenantA);

            assertThat(list).hasSize(1);
            assertThat(list.get(0).getPaymentStatus()).isEqualTo("PENDIENTE");
            assertThat(list.get(0).getInvoiceNumber()).isEqualTo("FAC-2026-001");
        }
    }
}
