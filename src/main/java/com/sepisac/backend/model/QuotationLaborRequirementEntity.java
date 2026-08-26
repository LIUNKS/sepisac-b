package com.sepisac.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "quotation_labor_requirements")
public class QuotationLaborRequirementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_id", nullable = false)
    private QuotationEntity quotation;

    @Column(name = "specialty_needed", nullable = false, length = 100)
    private String specialtyNeeded;

    @Column(name = "quantity_required", nullable = false)
    private Integer quantityRequired;

    @Column(name = "estimated_hours", nullable = false)
    private Integer estimatedHours;

    @Column(name = "locked_hourly_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal lockedHourlyCost;

    @Column(name = "subtotal_labor", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotalLabor;

    public QuotationLaborRequirementEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public QuotationEntity getQuotation() {
        return quotation;
    }

    public void setQuotation(QuotationEntity quotation) {
        this.quotation = quotation;
    }

    public String getSpecialtyNeeded() {
        return specialtyNeeded;
    }

    public void setSpecialtyNeeded(String specialtyNeeded) {
        this.specialtyNeeded = specialtyNeeded;
    }

    public Integer getQuantityRequired() {
        return quantityRequired;
    }

    public void setQuantityRequired(Integer quantityRequired) {
        this.quantityRequired = quantityRequired;
    }

    public Integer getEstimatedHours() {
        return estimatedHours;
    }

    public void setEstimatedHours(Integer estimatedHours) {
        this.estimatedHours = estimatedHours;
    }

    public BigDecimal getLockedHourlyCost() {
        return lockedHourlyCost;
    }

    public void setLockedHourlyCost(BigDecimal lockedHourlyCost) {
        this.lockedHourlyCost = lockedHourlyCost;
    }

    public BigDecimal getSubtotalLabor() {
        return subtotalLabor;
    }

    public void setSubtotalLabor(BigDecimal subtotalLabor) {
        this.subtotalLabor = subtotalLabor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QuotationLaborRequirementEntity that = (QuotationLaborRequirementEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
