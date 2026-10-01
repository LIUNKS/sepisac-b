package com.sepisac.backend.repository;

import com.sepisac.backend.model.ProjectAssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectAssignmentRepository extends JpaRepository<ProjectAssignmentEntity, UUID> {

    List<ProjectAssignmentEntity> findByProjectId(UUID projectId);

    List<ProjectAssignmentEntity> findByEmployeeId(UUID employeeId);
}
