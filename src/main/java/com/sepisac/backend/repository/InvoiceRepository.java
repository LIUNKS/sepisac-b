package com.sepisac.backend.repository;

import com.sepisac.backend.model.InvoiceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<InvoiceEntity, UUID> {

    Optional<InvoiceEntity> findByIdAndCompanyId(UUID id, UUID companyId);

    boolean existsByCompanyIdAndInvoiceNumber(UUID companyId, String invoiceNumber);

    List<InvoiceEntity> findByCompanyId(UUID companyId);

    List<InvoiceEntity> findByQuotationId(UUID quotationId);

    List<InvoiceEntity> findByCompanyIdAndPaymentStatus(UUID companyId, String paymentStatus);

    List<InvoiceEntity> findByPaymentStatus(String paymentStatus);

    @Query("SELECT i FROM InvoiceEntity i WHERE i.company.id = :companyId " +
            "AND (:status IS NULL OR i.paymentStatus = :status) " +
            "AND (:search IS NULL OR LOWER(i.invoiceNumber) LIKE :search OR LOWER(i.quotation.clientName) LIKE :search)")
    Page<InvoiceEntity> findByCompanyIdWithFilters(
            @Param("companyId") UUID companyId,
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT i FROM InvoiceEntity i WHERE " +
            "(:status IS NULL OR i.paymentStatus = :status) " +
            "AND (:search IS NULL OR LOWER(i.invoiceNumber) LIKE :search OR LOWER(i.quotation.clientName) LIKE :search)")
    Page<InvoiceEntity> findAllWithFiltersGlobal(
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable);
}
