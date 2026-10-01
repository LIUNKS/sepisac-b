package com.sepisac.backend.repository;

import com.sepisac.backend.model.PurchaseOrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrderEntity, UUID> {

    Optional<PurchaseOrderEntity> findByIdAndCompanyId(UUID id, UUID companyId);

    Optional<PurchaseOrderEntity> findByCompanyIdAndId(UUID companyId, UUID id);

    Page<PurchaseOrderEntity> findByCompanyIdAndStatusOrderByCreatedAtDesc(UUID companyId, String status, Pageable pageable);

    Page<PurchaseOrderEntity> findByCompanyIdOrderByCreatedAtDesc(UUID companyId, Pageable pageable);

    Optional<PurchaseOrderEntity> findByCompanyIdAndOrderNumber(UUID companyId, String orderNumber);

    boolean existsByCompanyIdAndOrderNumber(UUID companyId, String orderNumber);

    long countByCompanyIdAndOrderNumberStartingWith(UUID companyId, String prefix);

    @Query("SELECT DISTINCT d.purchaseOrder FROM PurchaseOrderDetailEntity d " +
           "WHERE d.purchaseOrder.company.id = :companyId " +
           "AND d.purchaseOrder.status = 'PENDIENTE' " +
           "AND (d.purchaseOrder.isDeleted = false OR d.purchaseOrder.isDeleted IS NULL) " +
           "AND d.inventoryItem.id IN :itemIds")
    List<PurchaseOrderEntity> findOpenPurchaseOrdersByItemIds(
            @Param("companyId") UUID companyId,
            @Param("itemIds") Collection<UUID> itemIds);

    @Query("SELECT DISTINCT d.inventoryItem.id FROM PurchaseOrderDetailEntity d " +
           "WHERE d.purchaseOrder.company.id = :companyId " +
           "AND d.purchaseOrder.status = 'PENDIENTE' " +
           "AND (d.purchaseOrder.isDeleted = false OR d.purchaseOrder.isDeleted IS NULL) " +
           "AND d.inventoryItem.id IN :itemIds")
    Set<UUID> findOpenPurchaseOrderItemIds(
            @Param("companyId") UUID companyId,
            @Param("itemIds") Collection<UUID> itemIds);
}
