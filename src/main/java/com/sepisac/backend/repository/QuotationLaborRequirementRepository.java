package com.sepisac.backend.repository;

import com.sepisac.backend.model.QuotationLaborRequirementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuotationLaborRequirementRepository extends JpaRepository<QuotationLaborRequirementEntity, UUID> {

    List<QuotationLaborRequirementEntity> findByQuotationId(UUID quotationId);

    void deleteByQuotationId(UUID quotationId);

    int countByQuotationId(UUID quotationId);

    @Query("SELECT l.quotation.id, count(l) FROM QuotationLaborRequirementEntity l WHERE l.quotation.id IN :quotationIds GROUP BY l.quotation.id")
    List<Object[]> countByQuotationIds(@Param("quotationIds") List<UUID> quotationIds);
}
