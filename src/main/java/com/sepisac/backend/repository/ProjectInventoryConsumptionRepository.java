package com.sepisac.backend.repository;

import com.sepisac.backend.model.ProjectInventoryConsumptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectInventoryConsumptionRepository extends JpaRepository<ProjectInventoryConsumptionEntity, UUID> {

    List<ProjectInventoryConsumptionEntity> findByProjectId(UUID projectId);

    List<ProjectInventoryConsumptionEntity> findByInventoryItemId(UUID inventoryItemId);
}
