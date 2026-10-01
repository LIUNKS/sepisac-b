package com.sepisac.backend.repository;

import com.sepisac.backend.model.AuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID>, JpaSpecificationExecutor<AuditLogEntity> {

    List<AuditLogEntity> findByCompanyId(UUID companyId);

    List<AuditLogEntity> findByUserId(UUID userId);

    List<AuditLogEntity> findByCompanyIdAndModuleAffectedAndEntityIdOrderByCreatedAtAsc(
            UUID companyId, String moduleAffected, UUID entityId);

    List<AuditLogEntity> findByCompanyIdAndModuleAffectedAndEntityIdAndActionOrderByCreatedAtDesc(
            UUID companyId, String moduleAffected, UUID entityId, String action);

    Optional<AuditLogEntity> findFirstByCompanyIdAndModuleAffectedAndEntityIdAndActionOrderByCreatedAtDesc(
            UUID companyId, String moduleAffected, UUID entityId, String action);

    @Query("SELECT a FROM AuditLogEntity a WHERE a.company.id = :companyId " +
            "AND (:moduleAffected IS NULL OR a.moduleAffected = :moduleAffected) " +
            "AND (:userId IS NULL OR a.user.id = :userId) " +
            "AND (:action IS NULL OR a.action = :action) " +
            "AND (:entityId IS NULL OR a.entityId = :entityId) " +
            "AND (cast(:from as java.time.OffsetDateTime) IS NULL OR a.createdAt >= :from) " +
            "AND (cast(:to as java.time.OffsetDateTime) IS NULL OR a.createdAt <= :to)")
    Page<AuditLogEntity> findByCompanyIdWithFilters(
            @Param("companyId") UUID companyId,
            @Param("moduleAffected") String moduleAffected,
            @Param("userId") UUID userId,
            @Param("action") String action,
            @Param("entityId") UUID entityId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            Pageable pageable);
}
