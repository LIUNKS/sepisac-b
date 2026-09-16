package com.sepisac.backend.repository;

import com.sepisac.backend.model.InventoryItemEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItemEntity, UUID> {

    boolean existsByCompanyIdAndSku(UUID companyId, String sku);

    List<InventoryItemEntity> findByCompanyId(UUID companyId);

    @Query("SELECT i FROM InventoryItemEntity i WHERE i.company.id = :companyId " +
            "AND (:lowStock IS NULL OR (:lowStock = true AND i.stockQuantity <= i.minStockAlert) OR (:lowStock = false AND i.stockQuantity > i.minStockAlert)) " +
            "AND (:search IS NULL OR (LOWER(i.sku) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%'))))")
    Page<InventoryItemEntity> findByCompanyIdWithFilters(
            @Param("companyId") UUID companyId,
            @Param("lowStock") Boolean lowStock,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT i FROM InventoryItemEntity i WHERE (:companyId IS NULL OR i.company.id = :companyId) " +
            "AND (:lowStock IS NULL OR (:lowStock = true AND i.stockQuantity <= i.minStockAlert) OR (:lowStock = false AND i.stockQuantity > i.minStockAlert)) " +
            "AND (:search IS NULL OR (LOWER(i.sku) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%'))))")
    Page<InventoryItemEntity> findAllWithFilters(
            @Param("companyId") UUID companyId,
            @Param("lowStock") Boolean lowStock,
            @Param("search") String search,
            Pageable pageable);
}
