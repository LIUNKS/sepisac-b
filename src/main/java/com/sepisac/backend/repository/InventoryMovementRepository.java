package com.sepisac.backend.repository;

import com.sepisac.backend.model.InventoryMovementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovementEntity, UUID> {

    List<InventoryMovementEntity> findByCompanyId(UUID companyId);

    List<InventoryMovementEntity> findByInventoryItemId(UUID inventoryItemId);
}
