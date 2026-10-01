package com.sepisac.backend.repository;

import com.sepisac.backend.model.ProjectMachineryAssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectMachineryAssignmentRepository extends JpaRepository<ProjectMachineryAssignmentEntity, UUID> {

    List<ProjectMachineryAssignmentEntity> findByProjectId(UUID projectId);

    List<ProjectMachineryAssignmentEntity> findByMachineryEquipmentId(UUID machineryEquipmentId);
}
