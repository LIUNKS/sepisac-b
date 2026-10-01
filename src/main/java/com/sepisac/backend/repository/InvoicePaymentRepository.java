package com.sepisac.backend.repository;

import com.sepisac.backend.model.InvoicePaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface InvoicePaymentRepository extends JpaRepository<InvoicePaymentEntity, UUID> {

    List<InvoicePaymentEntity> findByInvoiceId(UUID invoiceId);

    @Query("SELECT COALESCE(SUM(p.amountPaid), 0) FROM InvoicePaymentEntity p WHERE p.invoice.id = :invoiceId")
    BigDecimal sumAmountPaidByInvoiceId(@Param("invoiceId") UUID invoiceId);
}
