package com.sepisac.backend.service;

import com.sepisac.backend.dto.*;
import com.sepisac.backend.exception.ResourceNotFoundException;
import com.sepisac.backend.model.*;
import com.sepisac.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Sprint 7 - Dashboard & Audit Service Tests (D-T01 through D-T18)")
class DashboardAndAuditSprint7Test {

    @Mock
    private QuotationRepository quotationRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private InvoicePaymentRepository invoicePaymentRepository;

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private PurchaseOrderDetailRepository purchaseOrderDetailRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private AuditLogService auditLogService;

    private UUID companyId;
    private UUID userId;
    private CompanyEntity testCompany;
    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        userId = UUID.randomUUID();

        testCompany = new CompanyEntity();
        testCompany.setId(companyId);
        testCompany.setBusinessName("SEPI S.A.C.");

        testUser = new UserEntity();
        testUser.setId(userId);
        testUser.setUsername("gerente");
        testUser.setEmail("gerente@sepisac.com");
        testUser.setFullName("Gerente General");
        testUser.setIsDeleted(false);

        auditLogService = new AuditLogService(auditLogRepository, companyRepository, userRepository);
    }

    @Test
    @DisplayName("D-T01: RN-D01 - Date range validation: from > to or range > 366 days throws 400 VALIDATION_ERROR")
    void testDT01_dateRangeValidation() {
        LocalDate from = LocalDate.of(2026, 3, 15);
        LocalDate to = LocalDate.of(2026, 3, 10);

        assertThatThrownBy(() -> dashboardService.getCommercialCycleKpi(companyId, from, to))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("VALIDATION_ERROR");

        LocalDate fromFar = LocalDate.of(2024, 1, 1);
        LocalDate toFar = LocalDate.of(2025, 2, 1); // > 366 days

        assertThatThrownBy(() -> dashboardService.getCommercialCycleKpi(companyId, fromFar, toFar))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("366 days");
    }

    @Test
    @DisplayName("D-T02: RN-D02 - Base quotations filter: only status APROBADA, is_deleted=false within date range")
    void testDT02_baseQuotationsFilter() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);

        QuotationEntity q1 = new QuotationEntity();
        q1.setId(UUID.randomUUID());
        q1.setCompany(testCompany);
        q1.setStatus("APROBADA");
        q1.setIsDeleted(false);
        q1.setCreatedAt(OffsetDateTime.of(2026, 2, 1, 10, 0, 0, 0, ZoneOffset.UTC));

        when(quotationRepository.findApprovedQuotationsInRange(eq(companyId), any(), any()))
                .thenReturn(List.of(q1));

        CommercialCycleKpiResponseDTO result = dashboardService.getCommercialCycleKpi(companyId, from, to);

        assertThat(result).isNotNull();
        verify(quotationRepository).findApprovedQuotationsInRange(eq(companyId), any(), any());
    }

    @Test
    @DisplayName("D-T03: RN-D03 - Approval date resolution from audit_logs with action=APPROVE and module_affected=QUOTATIONS")
    void testDT03_approvalDateResolution() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);

        UUID qId = UUID.randomUUID();
        QuotationEntity q = new QuotationEntity();
        q.setId(qId);
        q.setCompany(testCompany);
        q.setStatus("APROBADA");
        q.setIsDeleted(false);
        q.setCreatedAt(OffsetDateTime.of(2026, 2, 1, 10, 0, 0, 0, ZoneOffset.UTC));

        AuditLogEntity approvalLog = new AuditLogEntity();
        approvalLog.setId(UUID.randomUUID());
        approvalLog.setAction("APPROVE");
        approvalLog.setModuleAffected("QUOTATIONS");
        approvalLog.setEntityId(qId);
        approvalLog.setCreatedAt(OffsetDateTime.of(2026, 2, 5, 12, 0, 0, 0, ZoneOffset.UTC));

        when(quotationRepository.findApprovedQuotationsInRange(eq(companyId), any(), any()))
                .thenReturn(List.of(q));
        when(auditLogRepository.findFirstByCompanyIdAndModuleAffectedAndEntityIdAndActionOrderByCreatedAtDesc(
                companyId, "QUOTATIONS", qId, "APPROVE"
        )).thenReturn(Optional.of(approvalLog));

        CommercialCycleKpiResponseDTO result = dashboardService.getCommercialCycleKpi(companyId, from, to);

        assertThat(result.getCreationToApproval().getSampleSize()).isEqualTo(1);
        // From 2026-02-01 to 2026-02-05 = 4 calendar days
        assertThat(result.getCreationToApproval().getAverageDays()).isEqualByComparingTo(new BigDecimal("4.00"));
    }

    @Test
    @DisplayName("D-T04: RN-D04 - Link with project start_date and invoice issue_date")
    void testDT04_linkProjectAndInvoiceDates() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);

        UUID qId = UUID.randomUUID();
        QuotationEntity q = new QuotationEntity();
        q.setId(qId);
        q.setCompany(testCompany);
        q.setStatus("APROBADA");
        q.setIsDeleted(false);
        q.setCreatedAt(OffsetDateTime.of(2026, 2, 1, 10, 0, 0, 0, ZoneOffset.UTC));

        AuditLogEntity approvalLog = new AuditLogEntity();
        approvalLog.setAction("APPROVE");
        approvalLog.setCreatedAt(OffsetDateTime.of(2026, 2, 3, 10, 0, 0, 0, ZoneOffset.UTC));

        ProjectEntity project = new ProjectEntity();
        project.setId(UUID.randomUUID());
        project.setQuotation(q);
        project.setStartDate(LocalDate.of(2026, 2, 10));
        project.setIsDeleted(false);

        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setId(UUID.randomUUID());
        invoice.setQuotation(q);
        invoice.setIssueDate(LocalDate.of(2026, 2, 20));
        invoice.setIsDeleted(false);

        when(quotationRepository.findApprovedQuotationsInRange(eq(companyId), any(), any()))
                .thenReturn(List.of(q));
        when(auditLogRepository.findFirstByCompanyIdAndModuleAffectedAndEntityIdAndActionOrderByCreatedAtDesc(
                companyId, "QUOTATIONS", qId, "APPROVE"
        )).thenReturn(Optional.of(approvalLog));
        when(projectRepository.findByQuotationId(qId)).thenReturn(List.of(project));
        when(invoiceRepository.findByQuotationId(qId)).thenReturn(List.of(invoice));

        CommercialCycleKpiResponseDTO result = dashboardService.getCommercialCycleKpi(companyId, from, to);

        // Approval to project start: 2026-02-03 to 2026-02-10 = 7 days
        assertThat(result.getApprovalToProjectStart().getAverageDays()).isEqualByComparingTo(new BigDecimal("7.00"));
        assertThat(result.getApprovalToProjectStart().getSampleSize()).isEqualTo(1);

        // Project start to invoice: 2026-02-10 to 2026-02-20 = 10 days
        assertThat(result.getProjectStartToInvoice().getAverageDays()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(result.getProjectStartToInvoice().getSampleSize()).isEqualTo(1);
    }

    @Test
    @DisplayName("D-T05: RN-D05 - Commercial cycle stage calculations: CREATION_TO_APPROVAL, APPROVAL_TO_PROJECT_START, PROJECT_START_TO_INVOICE, and TOTAL")
    void testDT05_commercialCycleStageCalculations() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);

        UUID qId = UUID.randomUUID();
        QuotationEntity q = new QuotationEntity();
        q.setId(qId);
        q.setCompany(testCompany);
        q.setStatus("APROBADA");
        q.setIsDeleted(false);
        q.setCreatedAt(OffsetDateTime.of(2026, 1, 10, 8, 0, 0, 0, ZoneOffset.UTC));

        AuditLogEntity approvalLog = new AuditLogEntity();
        approvalLog.setAction("APPROVE");
        approvalLog.setCreatedAt(OffsetDateTime.of(2026, 1, 15, 8, 0, 0, 0, ZoneOffset.UTC)); // 5 days

        ProjectEntity project = new ProjectEntity();
        project.setQuotation(q);
        project.setStartDate(LocalDate.of(2026, 1, 20)); // 5 days from approval
        project.setIsDeleted(false);

        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setQuotation(q);
        invoice.setIssueDate(LocalDate.of(2026, 1, 30)); // 10 days from project start
        invoice.setIsDeleted(false);

        when(quotationRepository.findApprovedQuotationsInRange(eq(companyId), any(), any()))
                .thenReturn(List.of(q));
        when(auditLogRepository.findFirstByCompanyIdAndModuleAffectedAndEntityIdAndActionOrderByCreatedAtDesc(
                companyId, "QUOTATIONS", qId, "APPROVE"
        )).thenReturn(Optional.of(approvalLog));
        when(projectRepository.findByQuotationId(qId)).thenReturn(List.of(project));
        when(invoiceRepository.findByQuotationId(qId)).thenReturn(List.of(invoice));

        CommercialCycleKpiResponseDTO result = dashboardService.getCommercialCycleKpi(companyId, from, to);

        assertThat(result.getCreationToApproval().getAverageDays()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(result.getApprovalToProjectStart().getAverageDays()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(result.getProjectStartToInvoice().getAverageDays()).isEqualByComparingTo(new BigDecimal("10.00"));
        // Total = 2026-01-10 to 2026-01-30 = 20 days
        assertThat(result.getTotal().getAverageDays()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(result.getTotal().getSampleSize()).isEqualTo(1);
    }

    @Test
    @DisplayName("D-T06: RN-D06 - Exclusion of negative differences: anomalies where end < start are ignored")
    void testDT06_excludeNegativeDifferences() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);

        UUID qId = UUID.randomUUID();
        QuotationEntity q = new QuotationEntity();
        q.setId(qId);
        q.setCompany(testCompany);
        q.setStatus("APROBADA");
        q.setIsDeleted(false);
        q.setCreatedAt(OffsetDateTime.of(2026, 2, 10, 10, 0, 0, 0, ZoneOffset.UTC));

        // Anomaly: approval date is before quotation creation date
        AuditLogEntity approvalLog = new AuditLogEntity();
        approvalLog.setAction("APPROVE");
        approvalLog.setCreatedAt(OffsetDateTime.of(2026, 2, 5, 10, 0, 0, 0, ZoneOffset.UTC));

        when(quotationRepository.findApprovedQuotationsInRange(eq(companyId), any(), any()))
                .thenReturn(List.of(q));
        when(auditLogRepository.findFirstByCompanyIdAndModuleAffectedAndEntityIdAndActionOrderByCreatedAtDesc(
                companyId, "QUOTATIONS", qId, "APPROVE"
        )).thenReturn(Optional.of(approvalLog));

        CommercialCycleKpiResponseDTO result = dashboardService.getCommercialCycleKpi(companyId, from, to);

        // Negative difference is excluded -> sampleSize = 0, averageDays = 0.00
        assertThat(result.getCreationToApproval().getSampleSize()).isEqualTo(0);
        assertThat(result.getCreationToApproval().getAverageDays()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("D-T07: RN-D07 - Average rounding HALF_UP to 2 decimal places and sample sizes per stage and total")
    void testDT07_roundingHalfUpAndSampleSizes() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);

        UUID qId1 = UUID.randomUUID();
        QuotationEntity q1 = new QuotationEntity();
        q1.setId(qId1);
        q1.setCompany(testCompany);
        q1.setStatus("APROBADA");
        q1.setIsDeleted(false);
        q1.setCreatedAt(OffsetDateTime.of(2026, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC));

        UUID qId2 = UUID.randomUUID();
        QuotationEntity q2 = new QuotationEntity();
        q2.setId(qId2);
        q2.setCompany(testCompany);
        q2.setStatus("APROBADA");
        q2.setIsDeleted(false);
        q2.setCreatedAt(OffsetDateTime.of(2026, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC));

        UUID qId3 = UUID.randomUUID();
        QuotationEntity q3 = new QuotationEntity();
        q3.setId(qId3);
        q3.setCompany(testCompany);
        q3.setStatus("APROBADA");
        q3.setIsDeleted(false);
        q3.setCreatedAt(OffsetDateTime.of(2026, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC));

        // Q1 approval: 1 day, Q2 approval: 2 days, Q3 approval: 2 days -> (1 + 2 + 2) / 3 = 1.666... -> 1.67
        AuditLogEntity log1 = new AuditLogEntity();
        log1.setCreatedAt(OffsetDateTime.of(2026, 1, 2, 10, 0, 0, 0, ZoneOffset.UTC));
        AuditLogEntity log2 = new AuditLogEntity();
        log2.setCreatedAt(OffsetDateTime.of(2026, 1, 3, 10, 0, 0, 0, ZoneOffset.UTC));
        AuditLogEntity log3 = new AuditLogEntity();
        log3.setCreatedAt(OffsetDateTime.of(2026, 1, 3, 10, 0, 0, 0, ZoneOffset.UTC));

        when(quotationRepository.findApprovedQuotationsInRange(eq(companyId), any(), any()))
                .thenReturn(List.of(q1, q2, q3));
        when(auditLogRepository.findFirstByCompanyIdAndModuleAffectedAndEntityIdAndActionOrderByCreatedAtDesc(
                companyId, "QUOTATIONS", qId1, "APPROVE"
        )).thenReturn(Optional.of(log1));
        when(auditLogRepository.findFirstByCompanyIdAndModuleAffectedAndEntityIdAndActionOrderByCreatedAtDesc(
                companyId, "QUOTATIONS", qId2, "APPROVE"
        )).thenReturn(Optional.of(log2));
        when(auditLogRepository.findFirstByCompanyIdAndModuleAffectedAndEntityIdAndActionOrderByCreatedAtDesc(
                companyId, "QUOTATIONS", qId3, "APPROVE"
        )).thenReturn(Optional.of(log3));

        CommercialCycleKpiResponseDTO result = dashboardService.getCommercialCycleKpi(companyId, from, to);

        assertThat(result.getCreationToApproval().getSampleSize()).isEqualTo(3);
        assertThat(result.getCreationToApproval().getAverageDays()).isEqualByComparingTo(new BigDecimal("1.67"));
    }

    @Test
    @DisplayName("D-T08: RN-D08 - Revenue KPI: grouped by day, month, year; separated by currency; totals by currency; never sums different currencies")
    void testDT08_revenueKpiGroupingAndCurrencySeparation() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);

        InvoiceEntity invPEN = new InvoiceEntity();
        invPEN.setCurrency("PEN");
        invPEN.setCompany(testCompany);

        InvoiceEntity invUSD = new InvoiceEntity();
        invUSD.setCurrency("USD");
        invUSD.setCompany(testCompany);

        InvoicePaymentEntity p1 = new InvoicePaymentEntity();
        p1.setInvoice(invPEN);
        p1.setAmountPaid(new BigDecimal("1000.00"));
        p1.setPaymentDate(OffsetDateTime.of(2026, 1, 15, 10, 0, 0, 0, ZoneOffset.UTC));

        InvoicePaymentEntity p2 = new InvoicePaymentEntity();
        p2.setInvoice(invPEN);
        p2.setAmountPaid(new BigDecimal("500.00"));
        p2.setPaymentDate(OffsetDateTime.of(2026, 2, 20, 10, 0, 0, 0, ZoneOffset.UTC));

        InvoicePaymentEntity p3 = new InvoicePaymentEntity();
        p3.setInvoice(invUSD);
        p3.setAmountPaid(new BigDecimal("200.00"));
        p3.setPaymentDate(OffsetDateTime.of(2026, 2, 25, 10, 0, 0, 0, ZoneOffset.UTC));

        when(invoicePaymentRepository.findPaymentsByCompanyIdAndDateRange(eq(companyId), any(), any()))
                .thenReturn(List.of(p1, p2, p3));

        RevenueKpiResponseDTO result = dashboardService.getRevenueKpi(companyId, from, to, "month");

        assertThat(result.getGroupBy()).isEqualTo("month");
        assertThat(result.getSeries()).hasSize(3); // (2026-01, PEN), (2026-02, PEN), (2026-02, USD)

        List<CurrencyTotalDTO> totals = result.getTotalsByCurrency();
        assertThat(totals).hasSize(2);

        CurrencyTotalDTO penTotal = totals.stream().filter(t -> "PEN".equals(t.getCurrency())).findFirst().orElseThrow();
        assertThat(penTotal.getTotalAmount()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(penTotal.getPaymentCount()).isEqualTo(2);

        CurrencyTotalDTO usdTotal = totals.stream().filter(t -> "USD".equals(t.getCurrency())).findFirst().orElseThrow();
        assertThat(usdTotal.getTotalAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(usdTotal.getPaymentCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("D-T09: RN-D09 - Receivables KPI: effective status (PAGADA, VENCIDA, PENDIENTE, PARCIAL), excludes PAGADA from balance list, includes paid block")
    void testDT09_receivablesKpiEffectiveStatus() {
        LocalDate today = LocalDate.now();

        // 1. Fully paid invoice -> PAGADA (should be in paidSummary only)
        InvoiceEntity invPaid = new InvoiceEntity();
        invPaid.setId(UUID.randomUUID());
        invPaid.setTotalAmount(new BigDecimal("500.00"));
        invPaid.setCurrency("PEN");
        invPaid.setPaymentStatus("PAGADA");
        invPaid.setDueDate(today.minusDays(10));
        invPaid.setIsDeleted(false);

        // 2. Overdue invoice with remaining balance -> VENCIDA
        InvoiceEntity invOverdue = new InvoiceEntity();
        invOverdue.setId(UUID.randomUUID());
        invOverdue.setTotalAmount(new BigDecimal("1000.00"));
        invOverdue.setCurrency("PEN");
        invOverdue.setPaymentStatus("PARCIAL");
        invOverdue.setDueDate(today.minusDays(5)); // overdue!
        invOverdue.setIsDeleted(false);

        // 3. Pending invoice with due date in future -> PENDIENTE
        InvoiceEntity invPending = new InvoiceEntity();
        invPending.setId(UUID.randomUUID());
        invPending.setTotalAmount(new BigDecimal("800.00"));
        invPending.setCurrency("PEN");
        invPending.setPaymentStatus("PENDIENTE");
        invPending.setDueDate(today.plusDays(15));
        invPending.setIsDeleted(false);

        when(invoiceRepository.findByCompanyId(companyId))
                .thenReturn(List.of(invPaid, invOverdue, invPending));
        when(invoicePaymentRepository.sumAmountPaidByInvoiceId(invPaid.getId()))
                .thenReturn(new BigDecimal("500.00"));
        when(invoicePaymentRepository.sumAmountPaidByInvoiceId(invOverdue.getId()))
                .thenReturn(new BigDecimal("300.00")); // balance 700.00
        when(invoicePaymentRepository.sumAmountPaidByInvoiceId(invPending.getId()))
                .thenReturn(BigDecimal.ZERO); // balance 800.00

        ReceivablesKpiResponseDTO result = dashboardService.getReceivablesKpi(companyId);

        // Assert PAGADA is excluded from statusGroups
        assertThat(result.getStatusGroups().stream().noneMatch(g -> "PAGADA".equals(g.getStatus()))).isTrue();

        // Check VENCIDA
        ReceivableStatusGroupDTO overdueGroup = result.getStatusGroups().stream()
                .filter(g -> "VENCIDA".equals(g.getStatus())).findFirst().orElseThrow();
        assertThat(overdueGroup.getTotalBalance()).isEqualByComparingTo(new BigDecimal("700.00"));
        assertThat(overdueGroup.getInvoiceCount()).isEqualTo(1);

        // Check PENDIENTE
        ReceivableStatusGroupDTO pendingGroup = result.getStatusGroups().stream()
                .filter(g -> "PENDIENTE".equals(g.getStatus())).findFirst().orElseThrow();
        assertThat(pendingGroup.getTotalBalance()).isEqualByComparingTo(new BigDecimal("800.00"));
        assertThat(pendingGroup.getInvoiceCount()).isEqualTo(1);

        // Check Paid Summary
        assertThat(result.getPaidSummary()).isNotNull();
        assertThat(result.getPaidSummary().getInvoiceCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("D-T10: RN-D10 - Inventory alerts KPI: critical items (stockQuantity <= minStockAlert) with open PENDIENTE POs and requested quantity")
    void testDT10_inventoryAlertsKpi() {
        InventoryItemEntity criticalItem = new InventoryItemEntity();
        UUID itemId = UUID.randomUUID();
        criticalItem.setId(itemId);
        criticalItem.setSku("MAT-CRIT-01");
        criticalItem.setName("Tubo PVC 2\"");
        criticalItem.setStockQuantity(2);
        criticalItem.setMinStockAlert(10);
        criticalItem.setIsDeleted(false);

        PurchaseOrderEntity openOrder = new PurchaseOrderEntity();
        UUID poId = UUID.randomUUID();
        openOrder.setId(poId);
        openOrder.setOrderNumber("OC-2026-000001");
        openOrder.setStatus("PENDIENTE");
        openOrder.setCreatedAt(OffsetDateTime.now());

        PurchaseOrderDetailEntity detail = new PurchaseOrderDetailEntity();
        detail.setPurchaseOrder(openOrder);
        detail.setInventoryItem(criticalItem);
        detail.setQuantity(25);

        when(inventoryItemRepository.findCriticalItemsByCompanyId(companyId))
                .thenReturn(List.of(criticalItem));
        when(purchaseOrderRepository.findOpenPurchaseOrdersByItemIds(eq(companyId), any()))
                .thenReturn(List.of(openOrder));
        when(purchaseOrderDetailRepository.findByPurchaseOrderId(poId))
                .thenReturn(List.of(detail));

        PageResponseDTO<InventoryAlertResponseDTO> result = dashboardService.getInventoryAlerts(companyId, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        InventoryAlertResponseDTO alert = result.getContent().get(0);
        assertThat(alert.getSku()).isEqualTo("MAT-CRIT-01");
        assertThat(alert.getStockQuantity()).isEqualTo(2);
        assertThat(alert.getMinStockAlert()).isEqualTo(10);
        assertThat(alert.getDeficit()).isEqualTo(8);
        assertThat(alert.isHasOpenOrder()).isTrue();
        assertThat(alert.getTotalRequestedQuantity()).isEqualTo(25);
        assertThat(alert.getOpenPurchaseOrders()).hasSize(1);
        assertThat(alert.getOpenPurchaseOrders().get(0).getOrderNumber()).isEqualTo("OC-2026-000001");
    }

    @Test
    @DisplayName("D-T11: RN-D11 - Multi-tenant isolation: ensures KPI queries are strictly scoped by companyId")
    void testDT11_multiTenantIsolation() {
        UUID otherCompanyId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);

        dashboardService.getCommercialCycleKpi(companyId, from, to);
        verify(quotationRepository).findApprovedQuotationsInRange(eq(companyId), any(), any());
        verify(quotationRepository, never()).findApprovedQuotationsInRange(eq(otherCompanyId), any(), any());

        dashboardService.getRevenueKpi(companyId, from, to, "month");
        verify(invoicePaymentRepository).findPaymentsByCompanyIdAndDateRange(eq(companyId), any(), any());
        verify(invoicePaymentRepository, never()).findPaymentsByCompanyIdAndDateRange(eq(otherCompanyId), any(), any());

        dashboardService.getReceivablesKpi(companyId);
        verify(invoiceRepository).findByCompanyId(eq(companyId));
        verify(invoiceRepository, never()).findByCompanyId(eq(otherCompanyId));
    }

    @Test
    @DisplayName("D-T12: RN-D12 - Empty datasets handling: returns 0 averages and empty structures without throwing NPE")
    void testDT12_emptyDatasetsGracefulHandling() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);

        when(quotationRepository.findApprovedQuotationsInRange(eq(companyId), any(), any()))
                .thenReturn(Collections.emptyList());
        CommercialCycleKpiResponseDTO commResult = dashboardService.getCommercialCycleKpi(companyId, from, to);
        assertThat(commResult.getCreationToApproval().getSampleSize()).isEqualTo(0);
        assertThat(commResult.getCreationToApproval().getAverageDays()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(commResult.getTotal().getSampleSize()).isEqualTo(0);

        when(invoicePaymentRepository.findPaymentsByCompanyIdAndDateRange(eq(companyId), any(), any()))
                .thenReturn(Collections.emptyList());
        RevenueKpiResponseDTO revResult = dashboardService.getRevenueKpi(companyId, from, to, "month");
        assertThat(revResult.getSeries()).isEmpty();
        assertThat(revResult.getTotalsByCurrency()).isEmpty();

        when(invoiceRepository.findByCompanyId(companyId))
                .thenReturn(Collections.emptyList());
        ReceivablesKpiResponseDTO recResult = dashboardService.getReceivablesKpi(companyId);
        assertThat(recResult.getStatusGroups()).isEmpty();
        assertThat(recResult.getPaidSummary().getInvoiceCount()).isEqualTo(0);

        when(inventoryItemRepository.findCriticalItemsByCompanyId(companyId))
                .thenReturn(Collections.emptyList());
        PageResponseDTO<InventoryAlertResponseDTO> invResult = dashboardService.getInventoryAlerts(companyId, PageRequest.of(0, 10));
        assertThat(invResult.getContent()).isEmpty();
        assertThat(invResult.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("D-T13: RN-D13 - Read-only transactional verification on DashboardService")
    void testDT13_readOnlyTransactionalAnnotation() {
        Transactional transactional = DashboardService.class.getAnnotation(Transactional.class);
        assertThat(transactional).isNotNull();
        assertThat(transactional.readOnly()).isTrue();
    }

    @Test
    @DisplayName("D-T14: RN-D14 - Audit log query with filters: companyId, module, userId, action, entityId, from, to, pagination")
    void testDT14_auditLogQueryWithFilters() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);
        UUID entityId = UUID.randomUUID();

        AuditLogEntity logEntity = new AuditLogEntity();
        logEntity.setId(UUID.randomUUID());
        logEntity.setUser(testUser);
        logEntity.setAction("APPROVE");
        logEntity.setModuleAffected("QUOTATIONS");
        logEntity.setEntityId(entityId);
        logEntity.setDescription("Cotización aprobada");
        logEntity.setCreatedAt(OffsetDateTime.now());

        Page<AuditLogEntity> paged = new PageImpl<>(List.of(logEntity), PageRequest.of(0, 20), 1);

        when(auditLogRepository.findByCompanyIdWithFilters(
                eq(companyId), eq("QUOTATIONS"), eq(userId), eq("APPROVE"), eq(entityId), any(), any(), any()
        )).thenReturn(paged);

        PageResponseDTO<AuditLogResponseDTO> response = auditLogService.getAuditLogs(
                companyId, "QUOTATIONS", userId, "APPROVE", entityId, from, to, PageRequest.of(0, 20)
        );

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getAction()).isEqualTo("APPROVE");
        assertThat(response.getContent().get(0).getUser()).isNotNull();
        assertThat(response.getContent().get(0).getUser().getUsername()).isEqualTo("gerente");
    }

    @Test
    @DisplayName("D-T15: RN-D15 - Audit log validation: invalid module or action throws 400 VALIDATION_ERROR")
    void testDT15_auditLogInvalidModuleOrAction() {
        assertThatThrownBy(() -> auditLogService.getAuditLogs(
                companyId, "UNKNOWN_MODULE", null, null, null, null, null, PageRequest.of(0, 20)
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("VALIDATION_ERROR");

        assertThatThrownBy(() -> auditLogService.getAuditLogs(
                companyId, "QUOTATIONS", null, "INVALID_ACTION", null, null, null, PageRequest.of(0, 20)
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("VALIDATION_ERROR");
    }

    @Test
    @DisplayName("D-T16: RN-D16 - Audit log pagination limit: max page size capped to 100")
    void testDT16_maxPageSizeLimit() {
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(auditLogRepository.findByCompanyIdWithFilters(eq(companyId), any(), any(), any(), any(), any(), any(), pageableCaptor.capture()))
                .thenReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 100), 0));

        auditLogService.getAuditLogs(companyId, null, null, null, null, null, null, PageRequest.of(0, 150));

        Pageable passedPageable = pageableCaptor.getValue();
        assertThat(passedPageable.getPageSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("D-T17: RN-D17 - Entity audit trail: chronological ASC order, 404 AUDIT_ENTITY_NOT_FOUND when empty")
    void testDT17_entityAuditTrailAscAndNotFound() {
        UUID entityId = UUID.randomUUID();

        // 1. Not found throws ResourceNotFoundException
        when(auditLogRepository.findByCompanyIdAndModuleAffectedAndEntityIdOrderByCreatedAtAsc(companyId, "QUOTATIONS", entityId))
                .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> auditLogService.getEntityAuditTrail(companyId, "QUOTATIONS", entityId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("AUDIT_ENTITY_NOT_FOUND");

        // 2. Chronological ASC order
        AuditLogEntity l1 = new AuditLogEntity();
        l1.setId(UUID.randomUUID());
        l1.setAction("CREATE");
        l1.setCreatedAt(OffsetDateTime.of(2026, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC));

        AuditLogEntity l2 = new AuditLogEntity();
        l2.setId(UUID.randomUUID());
        l2.setAction("APPROVE");
        l2.setCreatedAt(OffsetDateTime.of(2026, 1, 2, 10, 0, 0, 0, ZoneOffset.UTC));

        when(auditLogRepository.findByCompanyIdAndModuleAffectedAndEntityIdOrderByCreatedAtAsc(companyId, "QUOTATIONS", entityId))
                .thenReturn(List.of(l1, l2));

        List<AuditLogResponseDTO> trail = auditLogService.getEntityAuditTrail(companyId, "QUOTATIONS", entityId);

        assertThat(trail).hasSize(2);
        assertThat(trail.get(0).getAction()).isEqualTo("CREATE");
        assertThat(trail.get(1).getAction()).isEqualTo("APPROVE");
    }

    @Test
    @DisplayName("D-T18: RN-D18 - Audit log sanitization (sensitive fields removed) and deleted user handling (user: null)")
    void testDT18_sanitizationAndDeletedUser() {
        UserEntity deletedUser = new UserEntity();
        deletedUser.setId(UUID.randomUUID());
        deletedUser.setIsDeleted(true);

        Map<String, Object> oldValues = new HashMap<>();
        oldValues.put("username", "admin");
        oldValues.put("password", "secret123");
        oldValues.put("password_hash", "$2a$10$abc");
        oldValues.put("two_factor_code", "123456");

        Map<String, Object> newValues = new HashMap<>();
        newValues.put("username", "admin_updated");
        newValues.put("token", "jwt-token-string");

        AuditLogEntity logEntity = new AuditLogEntity();
        logEntity.setId(UUID.randomUUID());
        logEntity.setUser(deletedUser);
        logEntity.setAction("UPDATE");
        logEntity.setModuleAffected("USERS");
        logEntity.setOldValues(oldValues);
        logEntity.setNewValues(newValues);
        logEntity.setCreatedAt(OffsetDateTime.now());

        AuditLogResponseDTO dto = auditLogService.mapToDTO(logEntity);

        // Deleted user should return user: null
        assertThat(dto.getUser()).isNull();

        // Sensitive fields stripped from oldValues
        assertThat(dto.getOldValues()).doesNotContainKeys("password", "password_hash", "two_factor_code");
        assertThat(dto.getOldValues()).containsKey("username");

        // Sensitive fields stripped from newValues
        assertThat(dto.getNewValues()).doesNotContainKey("token");
        assertThat(dto.getNewValues()).containsKey("username");
    }
}
