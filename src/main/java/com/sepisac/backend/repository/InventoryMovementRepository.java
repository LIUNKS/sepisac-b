package com.sepisac.backend.repository;

import com.sepisac.backend.model.InventoryMovementEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovementEntity, UUID> {

    List<InventoryMovementEntity> findByCompanyId(UUID companyId);

    List<InventoryMovementEntity> findByInventoryItemId(UUID inventoryItemId);

    @Query("SELECT m FROM InventoryMovementEntity m WHERE m.company.id = :companyId " +
           "AND (:inventoryItemId IS NULL OR m.inventoryItem.id = :inventoryItemId) " +
           "AND (:movementType IS NULL OR UPPER(m.movementType) = UPPER(:movementType)) " +
           "AND (cast(:startDate as timestamp) IS NULL OR m.createdAt >= :startDate) " +
           "AND (cast(:endDate as timestamp) IS NULL OR m.createdAt <= :endDate)")
    Page<InventoryMovementEntity> findByCompanyIdWithFilters(
            @Param("companyId") UUID companyId,
            @Param("inventoryItemId") UUID inventoryItemId,
            @Param("movementType") String movementType,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            Pageable pageable);

    @Query("SELECT m FROM InventoryMovementEntity m WHERE " +
           "(:companyId IS NULL OR m.company.id = :companyId) " +
           "AND (:inventoryItemId IS NULL OR m.inventoryItem.id = :inventoryItemId) " +
           "AND (:movementType IS NULL OR UPPER(m.movementType) = UPPER(:movementType)) " +
           "AND (cast(:startDate as timestamp) IS NULL OR m.createdAt >= :startDate) " +
           "AND (cast(:endDate as timestamp) IS NULL OR m.createdAt <= :endDate)")
    Page<InventoryMovementEntity> findAllWithFiltersGlobal(
            @Param("companyId") UUID companyId,
            @Param("inventoryItemId") UUID inventoryItemId,
            @Param("movementType") String movementType,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            Pageable pageable);

    Page<InventoryMovementEntity> findByInventoryItemId(UUID inventoryItemId, Pageable pageable);
}
