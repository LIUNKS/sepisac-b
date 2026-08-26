package com.sepisac.backend.repository;

import com.sepisac.backend.model.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {

    List<AuditLogEntity> findByCompanyId(UUID companyId);

    List<AuditLogEntity> findByUserId(UUID userId);
}
