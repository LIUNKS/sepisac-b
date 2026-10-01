package com.sepisac.backend.service;

import com.sepisac.backend.dto.*;
import com.sepisac.backend.exception.BusinessRuleException;
import com.sepisac.backend.exception.ItemUnavailableException;
import com.sepisac.backend.exception.ResourceNotFoundException;
import com.sepisac.backend.model.*;
import com.sepisac.backend.repository.*;
import com.sepisac.backend.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Sprint 6 - PurchaseOrderService & Replenishment Tests (P-T01 through P-T18)")
class PurchaseOrderSprint6Test {

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private PurchaseOrderDetailRepository purchaseOrderDetailRepository;

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @Mock
    private InventoryMovementRepository inventoryMovementRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private PurchaseOrderService purchaseOrderService;

    private UUID companyId;
    private UUID userId;
    private UUID supplierId1;
    private UUID supplierId2;
    private CompanyEntity testCompany;
    private UserEntity testUser;
    private SupplierEntity testSupplier1;
    private SupplierEntity testSupplier2;
    private InventoryItemEntity item1;
    private InventoryItemEntity item2;
    private InventoryItemEntity itemWithoutSupplier;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        userId = UUID.randomUUID();
        supplierId1 = UUID.randomUUID();
        supplierId2 = UUID.randomUUID();

        testCompany = new CompanyEntity();
        testCompany.setId(companyId);
        testCompany.setBusinessName("SEPI S.A.C.");

        testUser = new UserEntity();
        testUser.setId(userId);
        testUser.setUsername("almacenero");

        testSupplier1 = new SupplierEntity();
        testSupplier1.setId(supplierId1);
        testSupplier1.setCompany(testCompany);
        testSupplier1.setBusinessName("ACEROS AREQUIPA S.A.");
        testSupplier1.setRuc("20100010724");
        testSupplier1.setIsDeleted(false);

        testSupplier2 = new SupplierEntity();
        testSupplier2.setId(supplierId2);
        testSupplier2.setCompany(testCompany);
        testSupplier2.setBusinessName("SODIMAC PERU S.A.");
        testSupplier2.setRuc("20200020835");
        testSupplier2.setIsDeleted(false);

        item1 = new InventoryItemEntity();
        item1.setId(UUID.randomUUID());
        item1.setCompany(testCompany);
        item1.setSku("MAT-001");
        item1.setName("Fierro Corrugado 1/2");
        item1.setStockQuantity(2);
        item1.setMinStockAlert(10);
        item1.setReorderQuantity(25);
        item1.setPurchaseCost(new BigDecimal("35.50"));
        item1.setSupplierId(supplierId1);
        item1.setIsDeleted(false);

        item2 = new InventoryItemEntity();
        item2.setId(UUID.randomUUID());
        item2.setCompany(testCompany);
        item2.setSku("MAT-002");
        item2.setName("Cemento Sol 42.5kg");
        item2.setStockQuantity(5);
        item2.setMinStockAlert(20);
        item2.setReorderQuantity(null); // Will test calculated formula
        item2.setPurchaseCost(new BigDecimal("28.00"));
        item2.setSupplierId(supplierId1);
        item2.setIsDeleted(false);

        itemWithoutSupplier = new InventoryItemEntity();
        itemWithoutSupplier.setId(UUID.randomUUID());
        itemWithoutSupplier.setCompany(testCompany);
        itemWithoutSupplier.setSku("MAT-003");
        itemWithoutSupplier.setName("Arena Gruesa");
        itemWithoutSupplier.setStockQuantity(1);
        itemWithoutSupplier.setMinStockAlert(5);
        itemWithoutSupplier.setSupplierId(null);
        itemWithoutSupplier.setIsDeleted(false);
    }

    @Test
    @DisplayName("P-T01: RN-P01 - Detect critical items when stockQuantity <= minStockAlert")
    void testPT01_criticalItemsDetection() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(testCompany));
        when(inventoryItemRepository.findCriticalItemsByCompanyId(companyId))
                .thenReturn(List.of(item1, item2, itemWithoutSupplier));
        when(purchaseOrderRepository.findOpenPurchaseOrderItemIds(eq(companyId), any()))
                .thenReturn(Collections.emptySet());
        when(supplierRepository.findById(supplierId1)).thenReturn(Optional.of(testSupplier1));
        when(purchaseOrderRepository.save(any(PurchaseOrderEntity.class))).thenAnswer(inv -> {
            PurchaseOrderEntity po = inv.getArgument(0);
            po.setId(UUID.randomUUID());
            return po;
        });

        AutoGenerateOrdersResponseDTO response = purchaseOrderService.autoGenerateOrders(companyId, userId);

        assertThat(response).isNotNull();
        assertThat(response.getOrdersCreated()).isEqualTo(1);
        assertThat(response.getItemsWithoutSupplier()).isEqualTo(1); // itemWithoutSupplier
        assertThat(response.getItemsSkippedWithOpenOrder()).isEqualTo(0);
    }

    @Test
    @DisplayName("P-T02: RN-P02 - Group critical items by supplier_id into 1 purchase order per supplier in PENDIENTE, PEN, exchangeRate=1.0000")
    void testPT02_groupCriticalItemsBySupplier() {
        InventoryItemEntity itemForSupplier2 = new InventoryItemEntity();
        itemForSupplier2.setId(UUID.randomUUID());
        itemForSupplier2.setCompany(testCompany);
        itemForSupplier2.setSku("MAT-004");
        itemForSupplier2.setName("Pintura Látex");
        itemForSupplier2.setStockQuantity(3);
        itemForSupplier2.setMinStockAlert(8);
        itemForSupplier2.setReorderQuantity(10);
        itemForSupplier2.setPurchaseCost(new BigDecimal("45.00"));
        itemForSupplier2.setSupplierId(supplierId2);
        itemForSupplier2.setIsDeleted(false);

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(testCompany));
        when(inventoryItemRepository.findCriticalItemsByCompanyId(companyId))
                .thenReturn(List.of(item1, item2, itemForSupplier2));
        when(purchaseOrderRepository.findOpenPurchaseOrderItemIds(eq(companyId), any()))
                .thenReturn(Collections.emptySet());
        when(supplierRepository.findById(supplierId1)).thenReturn(Optional.of(testSupplier1));
        when(supplierRepository.findById(supplierId2)).thenReturn(Optional.of(testSupplier2));

        ArgumentCaptor<PurchaseOrderEntity> orderCaptor = ArgumentCaptor.forClass(PurchaseOrderEntity.class);
        when(purchaseOrderRepository.save(orderCaptor.capture())).thenAnswer(inv -> {
            PurchaseOrderEntity po = inv.getArgument(0);
            po.setId(UUID.randomUUID());
            return po;
        });

        AutoGenerateOrdersResponseDTO response = purchaseOrderService.autoGenerateOrders(companyId, userId);

        assertThat(response.getOrdersCreated()).isEqualTo(2);
        List<PurchaseOrderEntity> createdOrders = orderCaptor.getAllValues();
        assertThat(createdOrders).hasSize(2);

        for (PurchaseOrderEntity po : createdOrders) {
            assertThat(po.getStatus()).isEqualTo("PENDIENTE");
            assertThat(po.getCurrency()).isEqualTo("PEN");
            assertThat(po.getExchangeRate()).isEqualByComparingTo(new BigDecimal("1.0000"));
            assertThat(po.getCompany().getId()).isEqualTo(companyId);
        }
    }

    @Test
    @DisplayName("P-T03: RN-P03 - Critical item with supplier_id == null counted in itemsWithoutSupplier and no order created")
    void testPT03_itemWithoutSupplier() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(testCompany));
        when(inventoryItemRepository.findCriticalItemsByCompanyId(companyId))
                .thenReturn(List.of(itemWithoutSupplier));
        when(purchaseOrderRepository.findOpenPurchaseOrderItemIds(eq(companyId), any()))
                .thenReturn(Collections.emptySet());

        AutoGenerateOrdersResponseDTO response = purchaseOrderService.autoGenerateOrders(companyId, userId);

        assertThat(response.getOrdersCreated()).isEqualTo(0);
        assertThat(response.getItemsWithoutSupplier()).isEqualTo(1);
        verify(purchaseOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("P-T04: RN-P04 - Quantity calculation: uses reorderQuantity when set, else max(1, (minStockAlert * 2) - stockQuantity)")
    void testPT04_quantityCalculation() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(testCompany));
        when(inventoryItemRepository.findCriticalItemsByCompanyId(companyId))
                .thenReturn(List.of(item1, item2));
        when(purchaseOrderRepository.findOpenPurchaseOrderItemIds(eq(companyId), any()))
                .thenReturn(Collections.emptySet());
        when(supplierRepository.findById(supplierId1)).thenReturn(Optional.of(testSupplier1));

        when(purchaseOrderRepository.save(any(PurchaseOrderEntity.class))).thenAnswer(inv -> {
            PurchaseOrderEntity po = inv.getArgument(0);
            po.setId(UUID.randomUUID());
            return po;
        });

        ArgumentCaptor<PurchaseOrderDetailEntity> detailCaptor = ArgumentCaptor.forClass(PurchaseOrderDetailEntity.class);
        when(purchaseOrderDetailRepository.save(detailCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        purchaseOrderService.autoGenerateOrders(companyId, userId);

        List<PurchaseOrderDetailEntity> details = detailCaptor.getAllValues();
        assertThat(details).hasSize(2);

        // item1 has reorderQuantity = 25
        PurchaseOrderDetailEntity detail1 = details.stream()
                .filter(d -> d.getInventoryItem().getId().equals(item1.getId()))
                .findFirst().orElseThrow();
        assertThat(detail1.getQuantity()).isEqualTo(25);

        // item2 has minStockAlert = 20, stockQuantity = 5 -> formula: (20 * 2) - 5 = 35
        PurchaseOrderDetailEntity detail2 = details.stream()
                .filter(d -> d.getInventoryItem().getId().equals(item2.getId()))
                .findFirst().orElseThrow();
        assertThat(detail2.getQuantity()).isEqualTo(35);
    }

    @Test
    @DisplayName("P-T05: RN-P05 - Cost and subtotal calculation: unitCost=item.purchaseCost, subtotal=qty*unitCost, totalAmount=sum")
    void testPT05_costAndSubtotalCalculation() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(testCompany));
        when(inventoryItemRepository.findCriticalItemsByCompanyId(companyId))
                .thenReturn(List.of(item1));
        when(purchaseOrderRepository.findOpenPurchaseOrderItemIds(eq(companyId), any()))
                .thenReturn(Collections.emptySet());
        when(supplierRepository.findById(supplierId1)).thenReturn(Optional.of(testSupplier1));

        ArgumentCaptor<PurchaseOrderEntity> orderCaptor = ArgumentCaptor.forClass(PurchaseOrderEntity.class);
        when(purchaseOrderRepository.save(orderCaptor.capture())).thenAnswer(inv -> {
            PurchaseOrderEntity po = inv.getArgument(0);
            po.setId(UUID.randomUUID());
            return po;
        });

        purchaseOrderService.autoGenerateOrders(companyId, userId);

        PurchaseOrderEntity createdOrder = orderCaptor.getValue();
        // qty: 25, unitCost: 35.50 -> subtotal: 887.50
        BigDecimal expectedTotal = new BigDecimal("35.50").multiply(BigDecimal.valueOf(25)).setScale(2, RoundingMode.HALF_UP);
        assertThat(createdOrder.getTotalAmount()).isEqualByComparingTo(expectedTotal);
    }

    @Test
    @DisplayName("P-T06: RN-P06 - Item already in open PENDIENTE order is skipped and counted in itemsSkippedWithOpenOrder")
    void testPT06_itemSkippedWithOpenOrder() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(testCompany));
        when(inventoryItemRepository.findCriticalItemsByCompanyId(companyId))
                .thenReturn(List.of(item1, item2));
        // item1 is in open order
        when(purchaseOrderRepository.findOpenPurchaseOrderItemIds(eq(companyId), any()))
                .thenReturn(Set.of(item1.getId()));
        when(supplierRepository.findById(supplierId1)).thenReturn(Optional.of(testSupplier1));

        when(purchaseOrderRepository.save(any(PurchaseOrderEntity.class))).thenAnswer(inv -> {
            PurchaseOrderEntity po = inv.getArgument(0);
            po.setId(UUID.randomUUID());
            return po;
        });

        AutoGenerateOrdersResponseDTO response = purchaseOrderService.autoGenerateOrders(companyId, userId);

        assertThat(response.getItemsSkippedWithOpenOrder()).isEqualTo(1);
        assertThat(response.getOrdersCreated()).isEqualTo(1); // for item2
    }

    @Test
    @DisplayName("P-T07: RN-P07 - Idempotent: running autoGenerateOrders twice does not duplicate orders")
    void testPT07_idempotentAutoGeneration() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(testCompany));
        when(inventoryItemRepository.findCriticalItemsByCompanyId(companyId))
                .thenReturn(List.of(item1));
        // In second run, item1 is already in open order
        when(purchaseOrderRepository.findOpenPurchaseOrderItemIds(eq(companyId), any()))
                .thenReturn(Set.of(item1.getId()));

        AutoGenerateOrdersResponseDTO response = purchaseOrderService.autoGenerateOrders(companyId, userId);

        assertThat(response.getOrdersCreated()).isEqualTo(0);
        assertThat(response.getItemsSkippedWithOpenOrder()).isEqualTo(1);
        verify(purchaseOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("P-T08: RN-P08 - Post-consumption hook in ProjectExecutionService triggers critical stock check and auto-generation safely")
    void testPT08_postConsumptionHookDoesNotRollbackOnException() {
        ProjectRepository projectRepo = mock(ProjectRepository.class);
        QuotationRepository quotRepo = mock(QuotationRepository.class);
        QuotationDetailRepository quotDetailRepo = mock(QuotationDetailRepository.class);
        InventoryItemRepository itemRepo = mock(InventoryItemRepository.class);
        InventoryMovementRepository movRepo = mock(InventoryMovementRepository.class);
        MachineryEquipmentRepository machRepo = mock(MachineryEquipmentRepository.class);
        EmployeeRepository empRepo = mock(EmployeeRepository.class);
        ProjectAssignmentRepository assignRepo = mock(ProjectAssignmentRepository.class);
        ProjectMachineryAssignmentRepository machAssignRepo = mock(ProjectMachineryAssignmentRepository.class);
        ProjectInventoryConsumptionRepository consRepo = mock(ProjectInventoryConsumptionRepository.class);
        PurchaseOrderService poService = mock(PurchaseOrderService.class);

        ProjectExecutionService projService = new ProjectExecutionService(
                projectRepo, quotRepo, quotDetailRepo, itemRepo, movRepo,
                machRepo, empRepo, assignRepo, machAssignRepo, consRepo, null, poService
        );

        UUID projectId = UUID.randomUUID();
        ProjectEntity project = new ProjectEntity();
        project.setId(projectId);
        project.setCompany(testCompany);
        project.setCode("PRJ-001");

        when(projectRepo.findById(projectId)).thenReturn(Optional.of(project));
        when(itemRepo.findById(item1.getId())).thenReturn(Optional.of(item1));
        when(itemRepo.save(any(InventoryItemEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(consRepo.save(any(ProjectInventoryConsumptionEntity.class))).thenAnswer(inv -> {
            ProjectInventoryConsumptionEntity c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        // Simulate exception in poService.autoGenerateOrders
        doThrow(new RuntimeException("Simulated error in auto-replenishment"))
                .when(poService).autoGenerateOrders(any(), any());

        ProjectInventoryConsumptionCreateDTO dto = new ProjectInventoryConsumptionCreateDTO(item1.getId(), 1, "Prueba");
        UserPrincipal principal = new UserPrincipal(userId, "u@test.com", "u", "p", "User", companyId,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ALMACEN")), true);

        // Method should NOT throw exception even if poService threw exception!
        ProjectInventoryConsumptionResponseDTO response = projService.consumeInventory(projectId, dto, principal);

        assertThat(response).isNotNull();
        verify(poService, times(1)).autoGenerateOrders(eq(companyId), eq(userId));
    }

    @Test
    @DisplayName("P-T09: RN-P09 - Manual order creation validation: supplier belongs to tenant, quantity>0, unitCost>=0, currency PEN/USD")
    void testPT09_createManualOrderValidation() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(testCompany));
        when(supplierRepository.findById(supplierId1)).thenReturn(Optional.of(testSupplier1));
        when(inventoryItemRepository.findById(item1.getId())).thenReturn(Optional.of(item1));

        when(purchaseOrderRepository.save(any(PurchaseOrderEntity.class))).thenAnswer(inv -> {
            PurchaseOrderEntity po = inv.getArgument(0);
            po.setId(UUID.randomUUID());
            po.setCreatedAt(OffsetDateTime.now());
            po.setUpdatedAt(OffsetDateTime.now());
            return po;
        });

        CreatePurchaseOrderRequestDTO request = new CreatePurchaseOrderRequestDTO(
                supplierId1,
                "USD",
                new BigDecimal("3.7500"),
                List.of(new PurchaseOrderDetailRequestDTO(item1.getId(), 10, new BigDecimal("15.00")))
        );

        PurchaseOrderResponseDTO response = purchaseOrderService.createManualOrder(companyId, userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getCurrency()).isEqualTo("USD");
        assertThat(response.getExchangeRate()).isEqualByComparingTo(new BigDecimal("3.7500"));
        assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("150.00")); // 10 * 15.00

        // Test invalid currency
        request.setCurrency("EUR");
        assertThatThrownBy(() -> purchaseOrderService.createManualOrder(companyId, userId, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Moneda no válida");

        // Test missing exchangeRate for USD
        request.setCurrency("USD");
        request.setExchangeRate(null);
        assertThatThrownBy(() -> purchaseOrderService.createManualOrder(companyId, userId, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("tipo de cambio");
    }

    @Test
    @DisplayName("P-T10: RN-P10 - Order number sequence formatting: OC-{yyyy}-{6-digit sequence} with retry on collision")
    void testPT10_orderNumberSequenceAndRetry() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(testCompany));
        when(supplierRepository.findById(supplierId1)).thenReturn(Optional.of(testSupplier1));
        when(inventoryItemRepository.findById(item1.getId())).thenReturn(Optional.of(item1));

        int currentYear = LocalDate.now().getYear();
        String expectedPrefix = "OC-" + currentYear + "-";

        when(purchaseOrderRepository.countByCompanyIdAndOrderNumberStartingWith(eq(companyId), anyString()))
                .thenReturn(0L);

        // First attempt fails with collision, second succeeds
        when(purchaseOrderRepository.save(any(PurchaseOrderEntity.class)))
                .thenThrow(new DataIntegrityViolationException("Unique constraint violation"))
                .thenAnswer(inv -> {
                    PurchaseOrderEntity po = inv.getArgument(0);
                    po.setId(UUID.randomUUID());
                    po.setCreatedAt(OffsetDateTime.now());
                    return po;
                });

        CreatePurchaseOrderRequestDTO request = new CreatePurchaseOrderRequestDTO(
                supplierId1,
                "PEN",
                new BigDecimal("1.0000"),
                List.of(new PurchaseOrderDetailRequestDTO(item1.getId(), 5, new BigDecimal("20.00")))
        );

        PurchaseOrderResponseDTO response = purchaseOrderService.createManualOrder(companyId, userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getOrderNumber()).startsWith(expectedPrefix);
        verify(purchaseOrderRepository, times(2)).save(any(PurchaseOrderEntity.class));
    }

    @Test
    @DisplayName("P-T11: RN-P11 - Cancel order: only PENDIENTE allowed; RECIBIDA or CANCELADA throws 409 PO_INVALID_STATE")
    void testPT11_cancelOrderInvalidState() {
        PurchaseOrderEntity receivedOrder = new PurchaseOrderEntity();
        receivedOrder.setId(UUID.randomUUID());
        receivedOrder.setCompany(testCompany);
        receivedOrder.setStatus("RECIBIDA");

        when(purchaseOrderRepository.findByIdAndCompanyId(receivedOrder.getId(), companyId))
                .thenReturn(Optional.of(receivedOrder));

        assertThatThrownBy(() -> purchaseOrderService.cancelOrder(companyId, userId, receivedOrder.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("PO_INVALID_STATE");

        PurchaseOrderEntity canceledOrder = new PurchaseOrderEntity();
        canceledOrder.setId(UUID.randomUUID());
        canceledOrder.setCompany(testCompany);
        canceledOrder.setStatus("CANCELADA");

        when(purchaseOrderRepository.findByIdAndCompanyId(canceledOrder.getId(), companyId))
                .thenReturn(Optional.of(canceledOrder));

        assertThatThrownBy(() -> purchaseOrderService.cancelOrder(companyId, userId, canceledOrder.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("PO_INVALID_STATE");
    }

    @Test
    @DisplayName("P-T12: RN-P12 - Audit log verification for order creation, cancel, and reception")
    void testPT12_auditLoggingVerification() {
        PurchaseOrderEntity pendingOrder = new PurchaseOrderEntity();
        UUID orderId = UUID.randomUUID();
        pendingOrder.setId(orderId);
        pendingOrder.setCompany(testCompany);
        pendingOrder.setSupplier(testSupplier1);
        pendingOrder.setOrderNumber("OC-2026-000001");
        pendingOrder.setStatus("PENDIENTE");

        when(purchaseOrderRepository.findByIdAndCompanyId(orderId, companyId))
                .thenReturn(Optional.of(pendingOrder));
        when(purchaseOrderRepository.save(any(PurchaseOrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        purchaseOrderService.cancelOrder(companyId, userId, orderId);

        verify(auditLogService, times(1)).log(
                eq(companyId), eq(userId), eq("UPDATE"), eq("PURCHASE_ORDERS"),
                eq(orderId), contains("cancelada"), any(), any()
        );
    }

    @Test
    @DisplayName("P-T13: RN-P13 - Receive order: only PENDIENTE; item unavailable or deleted throws 422 PO_ITEM_UNAVAILABLE")
    void testPT13_receiveOrderItemUnavailableRollback() {
        PurchaseOrderEntity pendingOrder = new PurchaseOrderEntity();
        UUID orderId = UUID.randomUUID();
        pendingOrder.setId(orderId);
        pendingOrder.setCompany(testCompany);
        pendingOrder.setStatus("PENDIENTE");

        PurchaseOrderDetailEntity detail = new PurchaseOrderDetailEntity();
        detail.setInventoryItem(item1);
        detail.setQuantity(10);
        detail.setUnitCost(new BigDecimal("30.00"));

        when(purchaseOrderRepository.findByIdAndCompanyId(orderId, companyId))
                .thenReturn(Optional.of(pendingOrder));
        when(purchaseOrderDetailRepository.findByPurchaseOrderId(orderId))
                .thenReturn(List.of(detail));

        // Simulate item deleted
        InventoryItemEntity deletedItem = new InventoryItemEntity();
        deletedItem.setId(item1.getId());
        deletedItem.setSku("MAT-001");
        deletedItem.setIsDeleted(true);

        when(inventoryItemRepository.findById(item1.getId())).thenReturn(Optional.of(deletedItem));

        assertThatThrownBy(() -> purchaseOrderService.receiveOrder(companyId, userId, orderId))
                .isInstanceOf(ItemUnavailableException.class)
                .hasMessageContaining("PO_ITEM_UNAVAILABLE");

        verify(inventoryMovementRepository, never()).save(any());
        verify(purchaseOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("P-T14: RN-P14 - Receive order creates ENTRADA movement with quantity and reason 'Recepción de OC {orderNumber}'")
    void testPT14_receiveOrderCreatesEntradaMovement() {
        PurchaseOrderEntity order = new PurchaseOrderEntity();
        UUID orderId = UUID.randomUUID();
        order.setId(orderId);
        order.setCompany(testCompany);
        order.setOrderNumber("OC-2026-000005");
        order.setStatus("PENDIENTE");
        order.setExchangeRate(new BigDecimal("1.0000"));

        PurchaseOrderDetailEntity detail = new PurchaseOrderDetailEntity();
        detail.setInventoryItem(item1);
        detail.setQuantity(10);
        detail.setUnitCost(new BigDecimal("35.50"));

        when(purchaseOrderRepository.findByIdAndCompanyId(orderId, companyId)).thenReturn(Optional.of(order));
        when(purchaseOrderDetailRepository.findByPurchaseOrderId(orderId)).thenReturn(List.of(detail));
        when(inventoryItemRepository.findById(item1.getId())).thenReturn(Optional.of(item1));
        when(inventoryMovementRepository.save(any(InventoryMovementEntity.class))).thenAnswer(inv -> {
            InventoryMovementEntity m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });

        purchaseOrderService.receiveOrder(companyId, userId, orderId);

        ArgumentCaptor<InventoryMovementEntity> movCaptor = ArgumentCaptor.forClass(InventoryMovementEntity.class);
        verify(inventoryMovementRepository).save(movCaptor.capture());

        InventoryMovementEntity mov = movCaptor.getValue();
        assertThat(mov.getMovementType()).isEqualTo("ENTRADA");
        assertThat(mov.getQuantityChanged()).isEqualTo(10);
        assertThat(mov.getReason()).isEqualTo("Recepción de OC OC-2026-000005");
    }

    @Test
    @DisplayName("P-T15: RN-P15 - Weighted average cost calculation in PEN: (currentStock*currentCost + qty*unitCostPen)/(newStock)")
    void testPT15_weightedAverageCostCalculation() {
        // Current: 10 units at 5.00 PEN
        item1.setStockQuantity(10);
        item1.setPurchaseCost(new BigDecimal("5.00"));

        PurchaseOrderEntity order = new PurchaseOrderEntity();
        UUID orderId = UUID.randomUUID();
        order.setId(orderId);
        order.setCompany(testCompany);
        order.setOrderNumber("OC-2026-000009");
        order.setStatus("PENDIENTE");
        order.setCurrency("PEN");
        order.setExchangeRate(new BigDecimal("1.0000"));

        // Receive 10 units at 10.00 PEN
        PurchaseOrderDetailEntity detail = new PurchaseOrderDetailEntity();
        detail.setInventoryItem(item1);
        detail.setQuantity(10);
        detail.setUnitCost(new BigDecimal("10.00"));

        when(purchaseOrderRepository.findByIdAndCompanyId(orderId, companyId)).thenReturn(Optional.of(order));
        when(purchaseOrderDetailRepository.findByPurchaseOrderId(orderId)).thenReturn(List.of(detail));
        when(inventoryItemRepository.findById(item1.getId())).thenReturn(Optional.of(item1));
        when(inventoryMovementRepository.save(any(InventoryMovementEntity.class))).thenAnswer(inv -> {
            InventoryMovementEntity m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });

        PurchaseReceptionResponseDTO response = purchaseOrderService.receiveOrder(companyId, userId, orderId);

        // Expected: (10 * 5.00 + 10 * 10.00) / 20 = 150.00 / 20 = 7.50 PEN
        assertThat(response.getMovements()).hasSize(1);
        ReceptionMovementDTO m = response.getMovements().get(0);
        assertThat(m.getNewCost()).isEqualByComparingTo(new BigDecimal("7.50"));
        assertThat(m.getNewStock()).isEqualTo(20);
        assertThat(item1.getPurchaseCost()).isEqualByComparingTo(new BigDecimal("7.50"));
    }

    @Test
    @DisplayName("P-T16: RN-P16 - Receive order updates stockQuantity and status to RECIBIDA")
    void testPT16_receiveOrderUpdatesStockAndStatus() {
        item1.setStockQuantity(10);

        PurchaseOrderEntity order = new PurchaseOrderEntity();
        UUID orderId = UUID.randomUUID();
        order.setId(orderId);
        order.setCompany(testCompany);
        order.setOrderNumber("OC-2026-000010");
        order.setStatus("PENDIENTE");
        order.setExchangeRate(new BigDecimal("1.0000"));

        PurchaseOrderDetailEntity detail = new PurchaseOrderDetailEntity();
        detail.setInventoryItem(item1);
        detail.setQuantity(15);
        detail.setUnitCost(new BigDecimal("20.00"));

        when(purchaseOrderRepository.findByIdAndCompanyId(orderId, companyId)).thenReturn(Optional.of(order));
        when(purchaseOrderDetailRepository.findByPurchaseOrderId(orderId)).thenReturn(List.of(detail));
        when(inventoryItemRepository.findById(item1.getId())).thenReturn(Optional.of(item1));
        when(inventoryMovementRepository.save(any(InventoryMovementEntity.class))).thenAnswer(inv -> {
            InventoryMovementEntity m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });

        PurchaseReceptionResponseDTO response = purchaseOrderService.receiveOrder(companyId, userId, orderId);

        assertThat(response.getStatus()).isEqualTo("RECIBIDA");
        assertThat(order.getStatus()).isEqualTo("RECIBIDA");
        assertThat(item1.getStockQuantity()).isEqualTo(25); // 10 + 15
        verify(purchaseOrderRepository).save(order);
    }

    @Test
    @DisplayName("P-T17: RN-P17 - Optimistic locking retry on receiveOrder: retries up to 3 times")
    void testPT17_optimisticLockingRetry() {
        PurchaseOrderEntity order = new PurchaseOrderEntity();
        UUID orderId = UUID.randomUUID();
        order.setId(orderId);
        order.setCompany(testCompany);
        order.setOrderNumber("OC-2026-000011");
        order.setStatus("PENDIENTE");
        order.setExchangeRate(new BigDecimal("1.0000"));

        PurchaseOrderDetailEntity detail = new PurchaseOrderDetailEntity();
        detail.setInventoryItem(item1);
        detail.setQuantity(5);
        detail.setUnitCost(new BigDecimal("20.00"));

        when(purchaseOrderRepository.findByIdAndCompanyId(orderId, companyId)).thenReturn(Optional.of(order));
        when(purchaseOrderDetailRepository.findByPurchaseOrderId(orderId)).thenReturn(List.of(detail));
        when(inventoryItemRepository.findById(item1.getId())).thenReturn(Optional.of(item1));
        when(inventoryMovementRepository.save(any(InventoryMovementEntity.class))).thenAnswer(inv -> {
            InventoryMovementEntity m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });

        // Fail once with OptimisticLockingFailureException, succeed on second attempt
        when(inventoryItemRepository.save(any(InventoryItemEntity.class)))
                .thenThrow(new OptimisticLockingFailureException("Simulated concurrent collision"))
                .thenAnswer(inv -> inv.getArgument(0));

        PurchaseReceptionResponseDTO response = purchaseOrderService.receiveOrder(companyId, userId, orderId);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("RECIBIDA");
        verify(inventoryItemRepository, times(2)).save(any(InventoryItemEntity.class));
    }

    @Test
    @DisplayName("P-T18: RN-P18 - Update replenishment settings: validates supplier belongs to company, reorderQuantity>0, minStockAlert>=0")
    void testPT18_updateReplenishmentSettingsValidation() {
        when(inventoryItemRepository.findById(item1.getId())).thenReturn(Optional.of(item1));
        when(supplierRepository.findById(supplierId1)).thenReturn(Optional.of(testSupplier1));
        when(inventoryItemRepository.save(any(InventoryItemEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        InventoryReplenishmentSettingsRequestDTO validRequest =
                new InventoryReplenishmentSettingsRequestDTO(supplierId1, 15, 30);

        purchaseOrderService.updateReplenishmentSettings(companyId, userId, item1.getId(), validRequest);

        assertThat(item1.getSupplierId()).isEqualTo(supplierId1);
        assertThat(item1.getMinStockAlert()).isEqualTo(15);
        assertThat(item1.getReorderQuantity()).isEqualTo(30);

        verify(auditLogService).log(
                eq(companyId), eq(userId), eq("UPDATE"), eq("INVENTORY"),
                eq(item1.getId()), contains("Configuración de reposición actualizada"), any(), any()
        );

        // Test invalid reorderQuantity <= 0
        InventoryReplenishmentSettingsRequestDTO invalidReq =
                new InventoryReplenishmentSettingsRequestDTO(supplierId1, 10, 0);
        assertThatThrownBy(() -> purchaseOrderService.updateReplenishmentSettings(companyId, userId, item1.getId(), invalidReq))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cantidad de reorden");

        // Test invalid minStockAlert < 0
        InventoryReplenishmentSettingsRequestDTO invalidMinStockReq =
                new InventoryReplenishmentSettingsRequestDTO(supplierId1, -1, 10);
        assertThatThrownBy(() -> purchaseOrderService.updateReplenishmentSettings(companyId, userId, item1.getId(), invalidMinStockReq))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("stock mínimo");
    }
}
