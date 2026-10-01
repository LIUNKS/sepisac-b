package com.sepisac.backend.repository;

import com.sepisac.backend.model.InvoicePaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface InvoicePaymentRepository extends JpaRepository<InvoicePaymentEntity, UUID> {

    List<InvoicePaymentEntity> findByInvoiceId(UUID invoiceId);

    @Query("SELECT COALESCE(SUM(p.amountPaid), 0) FROM InvoicePaymentEntity p WHERE p.invoice.id = :invoiceId")
    BigDecimal sumAmountPaidByInvoiceId(@Param("invoiceId") UUID invoiceId);

    @Query("SELECT p FROM InvoicePaymentEntity p WHERE p.invoice.company.id = :companyId " +
           "AND (p.invoice.isDeleted = false OR p.invoice.isDeleted IS NULL) " +
           "AND (cast(:from as java.time.OffsetDateTime) IS NULL OR p.paymentDate >= :from) " +
           "AND (cast(:to as java.time.OffsetDateTime) IS NULL OR p.paymentDate <= :to) " +
           "ORDER BY p.paymentDate ASC")
    List<InvoicePaymentEntity> findPaymentsByCompanyIdAndDateRange(
            @Param("companyId") UUID companyId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);

    @Query("SELECT p FROM InvoicePaymentEntity p WHERE p.invoice.company.id = :companyId " +
           "AND (p.invoice.isDeleted = false OR p.invoice.isDeleted IS NULL)")
    List<InvoicePaymentEntity> findByCompanyId(@Param("companyId") UUID companyId);
}
