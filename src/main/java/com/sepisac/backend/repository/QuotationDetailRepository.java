package com.sepisac.backend.repository;

import com.sepisac.backend.model.QuotationDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuotationDetailRepository extends JpaRepository<QuotationDetailEntity, UUID> {

    List<QuotationDetailEntity> findByQuotationId(UUID quotationId);

    void deleteByQuotationId(UUID quotationId);

    int countByQuotationId(UUID quotationId);

    @Query("SELECT d.quotation.id, count(d) FROM QuotationDetailEntity d WHERE d.quotation.id IN :quotationIds GROUP BY d.quotation.id")
    List<Object[]> countByQuotationIds(@Param("quotationIds") List<UUID> quotationIds);
}
