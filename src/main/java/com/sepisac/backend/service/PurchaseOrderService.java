package com.sepisac.backend.service;

import com.sepisac.backend.dto.*;
import com.sepisac.backend.exception.BusinessRuleException;
import com.sepisac.backend.exception.ItemUnavailableException;
import com.sepisac.backend.exception.ResourceNotFoundException;
import com.sepisac.backend.model.*;
import com.sepisac.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PurchaseOrderService {

    private static final Logger log = LoggerFactory.getLogger(PurchaseOrderService.class);

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderDetailRepository purchaseOrderDetailRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final SupplierRepository supplierRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public PurchaseOrderService(PurchaseOrderRepository purchaseOrderRepository,
                                PurchaseOrderDetailRepository purchaseOrderDetailRepository,
                                InventoryItemRepository inventoryItemRepository,
                                InventoryMovementRepository inventoryMovementRepository,
                                SupplierRepository supplierRepository,
                                CompanyRepository companyRepository,
                                UserRepository userRepository,
                                AuditLogService auditLogService) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.purchaseOrderDetailRepository = purchaseOrderDetailRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.supplierRepository = supplierRepository;
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * RN-P01 to RN-P07, RN-P10: Auto-generate purchase orders for critical items.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public AutoGenerateOrdersResponseDTO autoGenerateOrders(UUID companyId, UUID userId) {
        CompanyEntity company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada con ID: " + companyId));

        // RN-P01: critical items (stockQuantity <= minStockAlert, isDeleted = false)
        List<InventoryItemEntity> criticalItems = inventoryItemRepository.findCriticalItemsByCompanyId(companyId);

        if (criticalItems.isEmpty()) {
            return new AutoGenerateOrdersResponseDTO(0, 0, 0);
        }

        // RN-P06: find items already in open (PENDIENTE) purchase orders
        List<UUID> criticalItemIds = criticalItems.stream().map(InventoryItemEntity::getId).toList();
        Set<UUID> openOrderItemIds = purchaseOrderRepository.findOpenPurchaseOrderItemIds(companyId, criticalItemIds);

        int itemsSkippedWithOpenOrder = 0;
        int itemsWithoutSupplier = 0;
        int ordersCreated = 0;

        Map<UUID, List<InventoryItemEntity>> itemsBySupplier = new HashMap<>();

        for (InventoryItemEntity item : criticalItems) {
            if (openOrderItemIds != null && openOrderItemIds.contains(item.getId())) {
                itemsSkippedWithOpenOrder++;
                continue;
            }

            if (item.getSupplierId() == null) {
                itemsWithoutSupplier++;
                continue;
            }

            SupplierEntity supplier = supplierRepository.findById(item.getSupplierId()).orElse(null);
            if (supplier == null || Boolean.TRUE.equals(supplier.getIsDeleted()) || !supplier.getCompany().getId().equals(companyId)) {
                itemsWithoutSupplier++;
                continue;
            }

            itemsBySupplier.computeIfAbsent(item.getSupplierId(), k -> new ArrayList<>()).add(item);
        }

        // RN-P02: 1 order per supplier, PENDIENTE, PEN, exchangeRate=1.0000
        for (Map.Entry<UUID, List<InventoryItemEntity>> entry : itemsBySupplier.entrySet()) {
            UUID supplierId = entry.getKey();
            List<InventoryItemEntity> supplierItems = entry.getValue();

            SupplierEntity supplier = supplierRepository.findById(supplierId).orElse(null);
            if (supplier == null) {
                itemsWithoutSupplier += supplierItems.size();
                continue;
            }

            PurchaseOrderEntity savedOrder = createAutoPurchaseOrderWithRetry(company, supplier, supplierItems, companyId, userId);
            if (savedOrder != null) {
                ordersCreated++;
            }
        }

        return new AutoGenerateOrdersResponseDTO(ordersCreated, itemsWithoutSupplier, itemsSkippedWithOpenOrder);
    }

    private PurchaseOrderEntity createAutoPurchaseOrderWithRetry(CompanyEntity company, SupplierEntity supplier,
                                                                 List<InventoryItemEntity> items,
                                                                 UUID companyId, UUID userId) {
        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                String orderNumber = generateOrderNumber(companyId);

                PurchaseOrderEntity order = new PurchaseOrderEntity();
                order.setCompany(company);
                order.setSupplier(supplier);
                order.setOrderNumber(orderNumber);
                order.setCurrency("PEN");
                order.setExchangeRate(new BigDecimal("1.0000"));
                order.setStatus("PENDIENTE");
                order.setIsDeleted(false);

                BigDecimal totalAmount = BigDecimal.ZERO;
                List<PurchaseOrderDetailEntity> details = new ArrayList<>();

                for (InventoryItemEntity item : items) {
                    // RN-P04: quantity calculation
                    int quantity = item.getReorderQuantity() != null
                            ? item.getReorderQuantity()
                            : Math.max(1, (item.getMinStockAlert() * 2) - item.getStockQuantity());

                    // RN-P05: cost and subtotal
                    BigDecimal unitCost = item.getPurchaseCost() != null ? item.getPurchaseCost() : BigDecimal.ZERO;
                    BigDecimal subtotal = unitCost.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
                    totalAmount = totalAmount.add(subtotal);

                    PurchaseOrderDetailEntity detail = new PurchaseOrderDetailEntity();
                    detail.setPurchaseOrder(order);
                    detail.setInventoryItem(item);
                    detail.setQuantity(quantity);
                    detail.setUnitCost(unitCost);
                    detail.setSubtotal(subtotal);
                    details.add(detail);
                }

                order.setTotalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));
                PurchaseOrderEntity savedOrder = purchaseOrderRepository.save(order);

                for (PurchaseOrderDetailEntity detail : details) {
                    detail.setPurchaseOrder(savedOrder);
                    purchaseOrderDetailRepository.save(detail);
                }

                auditLogService.log(
                        companyId,
                        userId,
                        "CREATE",
                        "PURCHASE_ORDERS",
                        savedOrder.getId(),
                        "Orden de compra generada automáticamente: " + savedOrder.getOrderNumber(),
                        null,
                        null
                );

                return savedOrder;
            } catch (DataIntegrityViolationException e) {
                log.warn("Conflicto de número de orden en intento {}/{}: {}", attempt, maxAttempts, e.getMessage());
                if (attempt == maxAttempts) {
                    throw e;
                }
            }
        }
        return null;
    }

    /**
     * RN-P09, RN-P10: Manual order creation.
     */
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderResponseDTO createManualOrder(UUID companyId, UUID userId, CreatePurchaseOrderRequestDTO request) {
        CompanyEntity company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada con ID: " + companyId));

        if (request.getSupplierId() == null) {
            throw new BusinessRuleException("El proveedor es obligatorio");
        }

        SupplierEntity supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con ID: " + request.getSupplierId()));

        if (Boolean.TRUE.equals(supplier.getIsDeleted()) || !supplier.getCompany().getId().equals(companyId)) {
            throw new BusinessRuleException("El proveedor no pertenece a la empresa o ha sido eliminado");
        }

        if (request.getDetails() == null || request.getDetails().isEmpty()) {
            throw new BusinessRuleException("Los detalles de la orden no pueden estar vacíos");
        }

        String currency = request.getCurrency() != null ? request.getCurrency().trim().toUpperCase() : "PEN";
        if (!"PEN".equals(currency) && !"USD".equals(currency)) {
            throw new BusinessRuleException("Moneda no válida. Solo se permite PEN o USD");
        }

        BigDecimal exchangeRate;
        if ("PEN".equals(currency)) {
            exchangeRate = new BigDecimal("1.0000");
        } else {
            exchangeRate = request.getExchangeRate();
            if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRuleException("El tipo de cambio para USD debe ser mayor a 0");
            }
        }

        List<PurchaseOrderDetailEntity> detailsToSave = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (PurchaseOrderDetailRequestDTO detailDTO : request.getDetails()) {
            if (detailDTO.getQuantity() == null || detailDTO.getQuantity() <= 0) {
                throw new BusinessRuleException("La cantidad debe ser mayor a 0");
            }
            if (detailDTO.getUnitCost() == null || detailDTO.getUnitCost().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessRuleException("El costo unitario no puede ser negativo");
            }

            InventoryItemEntity item = inventoryItemRepository.findById(detailDTO.getInventoryItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ítem de inventario no encontrado con ID: " + detailDTO.getInventoryItemId()));

            if (Boolean.TRUE.equals(item.getIsDeleted()) || !item.getCompany().getId().equals(companyId)) {
                throw new BusinessRuleException("El ítem " + item.getSku() + " no pertenece a la empresa o ha sido eliminado");
            }

            BigDecimal subtotal = detailDTO.getUnitCost().multiply(BigDecimal.valueOf(detailDTO.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            totalAmount = totalAmount.add(subtotal);

            PurchaseOrderDetailEntity detail = new PurchaseOrderDetailEntity();
            detail.setInventoryItem(item);
            detail.setQuantity(detailDTO.getQuantity());
            detail.setUnitCost(detailDTO.getUnitCost());
            detail.setSubtotal(subtotal);
            detailsToSave.add(detail);
        }

        PurchaseOrderEntity savedOrder = null;
        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                String orderNumber = generateOrderNumber(companyId);

                PurchaseOrderEntity order = new PurchaseOrderEntity();
                order.setCompany(company);
                order.setSupplier(supplier);
                order.setOrderNumber(orderNumber);
                order.setCurrency(currency);
                order.setExchangeRate(exchangeRate);
                order.setStatus("PENDIENTE");
                order.setTotalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));
                order.setIsDeleted(false);

                savedOrder = purchaseOrderRepository.save(order);

                for (PurchaseOrderDetailEntity detail : detailsToSave) {
                    detail.setPurchaseOrder(savedOrder);
                    purchaseOrderDetailRepository.save(detail);
                }
                break;
            } catch (DataIntegrityViolationException e) {
                log.warn("Conflicto de número de orden manual en intento {}/{}: {}", attempt, maxAttempts, e.getMessage());
                if (attempt == maxAttempts) {
                    throw e;
                }
            }
        }

        auditLogService.log(
                companyId,
                userId,
                "CREATE",
                "PURCHASE_ORDERS",
                savedOrder.getId(),
                "Orden de compra creada manualmente: " + savedOrder.getOrderNumber(),
                null,
                null
        );

        return mapToDTO(savedOrder, detailsToSave);
    }

    /**
     * Get paginated orders by status.
     */
    @Transactional(readOnly = true)
    public PageResponseDTO<PurchaseOrderResponseDTO> getOrdersByStatus(UUID companyId, String status, Pageable pageable) {
        Page<PurchaseOrderEntity> page;
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) {
            page = purchaseOrderRepository.findByCompanyIdAndStatusOrderByCreatedAtDesc(companyId, status.toUpperCase().trim(), pageable);
        } else {
            page = purchaseOrderRepository.findByCompanyIdOrderByCreatedAtDesc(companyId, pageable);
        }

        List<PurchaseOrderResponseDTO> dtos = page.getContent().stream()
                .map(po -> {
                    List<PurchaseOrderDetailEntity> details = purchaseOrderDetailRepository.findByPurchaseOrderId(po.getId());
                    return mapToDTO(po, details);
                })
                .toList();

        return new PageResponseDTO<>(
                dtos,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    /**
     * Get order by ID.
     */
    @Transactional(readOnly = true)
    public PurchaseOrderResponseDTO getOrderById(UUID companyId, UUID id) {
        PurchaseOrderEntity order = purchaseOrderRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de compra no encontrada con ID: " + id));

        List<PurchaseOrderDetailEntity> details = purchaseOrderDetailRepository.findByPurchaseOrderId(id);
        return mapToDTO(order, details);
    }

    /**
     * RN-P11: Cancel purchase order (only PENDIENTE -> CANCELADA allowed).
     */
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderResponseDTO cancelOrder(UUID companyId, UUID userId, UUID id) {
        PurchaseOrderEntity order = purchaseOrderRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de compra no encontrada con ID: " + id));

        if (!"PENDIENTE".equalsIgnoreCase(order.getStatus())) {
            throw new BusinessRuleException("PO_INVALID_STATE: Solo se pueden cancelar órdenes en estado PENDIENTE. Estado actual: " + order.getStatus());
        }

        order.setStatus("CANCELADA");
        PurchaseOrderEntity saved = purchaseOrderRepository.save(order);

        auditLogService.log(
                companyId,
                userId,
                "UPDATE",
                "PURCHASE_ORDERS",
                order.getId(),
                "Orden de compra cancelada: " + order.getOrderNumber(),
                null,
                null
        );

        List<PurchaseOrderDetailEntity> details = purchaseOrderDetailRepository.findByPurchaseOrderId(id);
        return mapToDTO(saved, details);
    }

    /**
     * RN-P13 through RN-P17: Receive purchase order with 3x retry on OptimisticLockingFailureException.
     */
    public PurchaseReceptionResponseDTO receiveOrder(UUID companyId, UUID userId, UUID id) {
        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return executeReceiveOrder(companyId, userId, id);
            } catch (OptimisticLockingFailureException | jakarta.persistence.OptimisticLockException e) {
                log.warn("Optimistic locking failure al recibir OC {} (intento {}/{}): {}", id, attempt, maxAttempts, e.getMessage());
                if (attempt == maxAttempts) {
                    throw e;
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        throw new IllegalStateException("No se pudo completar la recepción tras " + maxAttempts + " intentos");
    }

    @Transactional(rollbackFor = Exception.class)
    protected PurchaseReceptionResponseDTO executeReceiveOrder(UUID companyId, UUID userId, UUID id) {
        PurchaseOrderEntity order = purchaseOrderRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de compra no encontrada con ID: " + id));

        // RN-P13: only PENDIENTE can be received
        if (!"PENDIENTE".equalsIgnoreCase(order.getStatus())) {
            throw new BusinessRuleException("PO_INVALID_STATE: Solo se pueden recibir órdenes en estado PENDIENTE. Estado actual: " + order.getStatus());
        }

        List<PurchaseOrderDetailEntity> details = purchaseOrderDetailRepository.findByPurchaseOrderId(id);
        if (details.isEmpty()) {
            throw new BusinessRuleException("La orden de compra no cuenta con detalles registrados");
        }

        // RN-P13: Validate all items exist and are not deleted before making any changes
        for (PurchaseOrderDetailEntity detail : details) {
            InventoryItemEntity item = inventoryItemRepository.findById(detail.getInventoryItem().getId())
                    .orElseThrow(() -> new ItemUnavailableException("PO_ITEM_UNAVAILABLE: Ítem no disponible con ID: " + detail.getInventoryItem().getId()));

            if (Boolean.TRUE.equals(item.getIsDeleted())) {
                throw new ItemUnavailableException("PO_ITEM_UNAVAILABLE: El ítem " + item.getSku() + " ha sido eliminado");
            }
        }

        UserEntity user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }

        List<ReceptionMovementDTO> movementDTOs = new ArrayList<>();
        OffsetDateTime receptionTime = OffsetDateTime.now();

        for (PurchaseOrderDetailEntity detail : details) {
            InventoryItemEntity item = inventoryItemRepository.findById(detail.getInventoryItem().getId()).get();

            int qtyReceived = detail.getQuantity();
            int previousStock = item.getStockQuantity();
            BigDecimal previousCost = item.getPurchaseCost() != null ? item.getPurchaseCost() : BigDecimal.ZERO;

            // RN-P14: register movement ENTRADA
            InventoryMovementEntity movement = new InventoryMovementEntity();
            movement.setCompany(order.getCompany());
            movement.setInventoryItem(item);
            movement.setUser(user);
            movement.setMovementType("ENTRADA");
            movement.setQuantityChanged(qtyReceived);
            movement.setReason("Recepción de OC " + order.getOrderNumber());
            InventoryMovementEntity savedMovement = inventoryMovementRepository.save(movement);

            // RN-P15: Weighted average cost in PEN
            BigDecimal unitCostPen = detail.getUnitCost().multiply(order.getExchangeRate());
            int newStock = previousStock + qtyReceived;

            BigDecimal totalCurrentValue = previousCost.multiply(BigDecimal.valueOf(previousStock));
            BigDecimal totalReceivedValue = unitCostPen.multiply(BigDecimal.valueOf(qtyReceived));
            BigDecimal newPurchaseCost = totalCurrentValue.add(totalReceivedValue)
                    .divide(BigDecimal.valueOf(newStock), 2, RoundingMode.HALF_UP);

            // RN-P16: update stock quantity and purchase cost
            item.setStockQuantity(newStock);
            item.setPurchaseCost(newPurchaseCost);
            inventoryItemRepository.save(item);

            movementDTOs.add(new ReceptionMovementDTO(
                    savedMovement.getId(),
                    item.getId(),
                    item.getSku(),
                    item.getName(),
                    qtyReceived,
                    previousStock,
                    newStock,
                    previousCost,
                    newPurchaseCost
            ));
        }

        // RN-P16: mark order as RECIBIDA
        order.setStatus("RECIBIDA");
        purchaseOrderRepository.save(order);

        auditLogService.log(
                companyId,
                userId,
                "UPDATE",
                "PURCHASE_ORDERS",
                order.getId(),
                "Recepción completada para OC: " + order.getOrderNumber(),
                null,
                null
        );

        return new PurchaseReceptionResponseDTO(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                receptionTime,
                movementDTOs
        );
    }

    /**
     * Update inventory replenishment settings.
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateReplenishmentSettings(UUID companyId, UUID userId, UUID itemId,
                                            InventoryReplenishmentSettingsRequestDTO request) {
        InventoryItemEntity item = inventoryItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Ítem de inventario no encontrado con ID: " + itemId));

        if (Boolean.TRUE.equals(item.getIsDeleted()) || !item.getCompany().getId().equals(companyId)) {
            throw new BusinessRuleException("El ítem no pertenece a la empresa o ha sido eliminado");
        }

        if (request.getSupplierId() != null) {
            SupplierEntity supplier = supplierRepository.findById(request.getSupplierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con ID: " + request.getSupplierId()));
            if (Boolean.TRUE.equals(supplier.getIsDeleted()) || !supplier.getCompany().getId().equals(companyId)) {
                throw new BusinessRuleException("El proveedor no pertenece a la empresa o ha sido eliminado");
            }
        }

        if (request.getReorderQuantity() == null || request.getReorderQuantity() <= 0) {
            throw new BusinessRuleException("La cantidad de reorden debe ser mayor a 0");
        }

        if (request.getMinStockAlert() == null || request.getMinStockAlert() < 0) {
            throw new BusinessRuleException("El stock mínimo de alerta no puede ser negativo");
        }

        item.setSupplierId(request.getSupplierId());
        item.setMinStockAlert(request.getMinStockAlert());
        item.setReorderQuantity(request.getReorderQuantity());
        inventoryItemRepository.save(item);

        auditLogService.log(
                companyId,
                userId,
                "UPDATE",
                "INVENTORY",
                item.getId(),
                "Configuración de reposición actualizada para ítem: " + item.getSku(),
                null,
                null
        );
    }

    /**
     * Helper to generate unique order number: OC-{yyyy}-{6-digit sequence}
     */
    private String generateOrderNumber(UUID companyId) {
        int currentYear = LocalDate.now().getYear();
        String prefix = "OC-" + currentYear + "-";
        long count = purchaseOrderRepository.countByCompanyIdAndOrderNumberStartingWith(companyId, prefix) + 1;
        String candidate = String.format("%s%06d", prefix, count);

        while (purchaseOrderRepository.existsByCompanyIdAndOrderNumber(companyId, candidate)) {
            count++;
            candidate = String.format("%s%06d", prefix, count);
        }
        return candidate;
    }

    private PurchaseOrderResponseDTO mapToDTO(PurchaseOrderEntity order, List<PurchaseOrderDetailEntity> details) {
        List<PurchaseOrderDetailResponseDTO> detailDTOs = details != null ? details.stream()
                .map(d -> new PurchaseOrderDetailResponseDTO(
                        d.getId(),
                        d.getInventoryItem().getId(),
                        d.getInventoryItem().getSku(),
                        d.getInventoryItem().getName(),
                        d.getQuantity(),
                        d.getUnitCost(),
                        d.getSubtotal()
                ))
                .toList() : Collections.emptyList();

        return new PurchaseOrderResponseDTO(
                order.getId(),
                order.getCompany().getId(),
                order.getSupplier().getId(),
                order.getSupplier().getBusinessName(),
                order.getSupplier().getRuc(),
                order.getOrderNumber(),
                order.getCurrency(),
                order.getExchangeRate(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                detailDTOs
        );
    }
}
