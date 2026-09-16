package com.sepisac.backend.repository;

import com.sepisac.backend.model.QuotationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuotationRepository extends JpaRepository<QuotationEntity, UUID> {

    boolean existsByCompanyIdAndQuotationNumber(UUID companyId, String quotationNumber);

    List<QuotationEntity> findByCompanyId(UUID companyId);

    long countByCompanyId(UUID companyId);

    @Query("SELECT q FROM QuotationEntity q WHERE q.company.id = :companyId " +
            "AND (:status IS NULL OR q.status = :status) " +
            "AND (:serviceType IS NULL OR q.serviceType = :serviceType) " +
            "AND (:search IS NULL OR (LOWER(q.quotationNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(q.clientName) LIKE LOWER(CONCAT('%', :search, '%'))))")
    Page<QuotationEntity> findByCompanyIdWithFilters(
            @Param("companyId") UUID companyId,
            @Param("status") String status,
            @Param("serviceType") String serviceType,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT q FROM QuotationEntity q WHERE (:companyId IS NULL OR q.company.id = :companyId) " +
            "AND (:status IS NULL OR q.status = :status) " +
            "AND (:serviceType IS NULL OR q.serviceType = :serviceType) " +
            "AND (:search IS NULL OR (LOWER(q.quotationNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(q.clientName) LIKE LOWER(CONCAT('%', :search, '%'))))")
    Page<QuotationEntity> findAllWithFilters(
            @Param("companyId") UUID companyId,
            @Param("status") String status,
            @Param("serviceType") String serviceType,
            @Param("search") String search,
            Pageable pageable);
}
