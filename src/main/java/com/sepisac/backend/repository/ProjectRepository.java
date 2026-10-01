package com.sepisac.backend.repository;

import com.sepisac.backend.model.ProjectEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<ProjectEntity, UUID> {

    boolean existsByCompanyIdAndCode(UUID companyId, String code);

    List<ProjectEntity> findByCompanyId(UUID companyId);

    List<ProjectEntity> findByQuotationId(UUID quotationId);

    long countByCompanyId(UUID companyId);

    @Query("SELECT p FROM ProjectEntity p WHERE p.company.id = :companyId " +
            "AND (:status IS NULL OR p.status = :status) " +
            "AND (:search IS NULL OR LOWER(p.code) LIKE :search OR LOWER(p.title) LIKE :search OR LOWER(p.clientName) LIKE :search)")
    Page<ProjectEntity> findByCompanyIdWithFilters(
            @Param("companyId") UUID companyId,
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT p FROM ProjectEntity p WHERE " +
            "(:status IS NULL OR p.status = :status) " +
            "AND (:search IS NULL OR LOWER(p.code) LIKE :search OR LOWER(p.title) LIKE :search OR LOWER(p.clientName) LIKE :search)")
    Page<ProjectEntity> findAllWithFiltersGlobal(
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable);
}
