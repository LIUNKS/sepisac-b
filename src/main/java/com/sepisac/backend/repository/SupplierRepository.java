package com.sepisac.backend.repository;

import com.sepisac.backend.model.SupplierEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SupplierRepository extends JpaRepository<SupplierEntity, UUID> {

    boolean existsByCompanyIdAndRuc(UUID companyId, String ruc);

    List<SupplierEntity> findByCompanyId(UUID companyId);

    @Query("SELECT s FROM SupplierEntity s WHERE s.company.id = :companyId " +
            "AND (:search IS NULL OR (LOWER(s.ruc) LIKE LOWER(CONCAT('%', cast(:search as string), '%')) OR LOWER(s.businessName) LIKE LOWER(CONCAT('%', cast(:search as string), '%'))))")
    Page<SupplierEntity> findByCompanyIdWithFilters(
            @Param("companyId") UUID companyId,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT s FROM SupplierEntity s WHERE (:companyId IS NULL OR s.company.id = :companyId) " +
            "AND (:search IS NULL OR (LOWER(s.ruc) LIKE LOWER(CONCAT('%', cast(:search as string), '%')) OR LOWER(s.businessName) LIKE LOWER(CONCAT('%', cast(:search as string), '%'))))")
    Page<SupplierEntity> findAllWithFilters(
            @Param("companyId") UUID companyId,
            @Param("search") String search,
            Pageable pageable);
}
