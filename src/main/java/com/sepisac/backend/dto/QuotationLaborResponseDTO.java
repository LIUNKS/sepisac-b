package com.sepisac.backend.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class QuotationLaborResponseDTO {

    private UUID id;
    private UUID quotationId;
    private String specialtyNeeded;
    private Integer quantityRequired;
    private Integer estimatedHours;
    private BigDecimal lockedHourlyCost;
    private BigDecimal subtotalLabor;

    public QuotationLaborResponseDTO() {
    }

    public QuotationLaborResponseDTO(UUID id, UUID quotationId, String specialtyNeeded, Integer quantityRequired,
                                     Integer estimatedHours, BigDecimal lockedHourlyCost, BigDecimal subtotalLabor) {
        this.id = id;
        this.quotationId = quotationId;
        this.specialtyNeeded = specialtyNeeded;
        this.quantityRequired = quantityRequired;
        this.estimatedHours = estimatedHours;
        this.lockedHourlyCost = lockedHourlyCost;
        this.subtotalLabor = subtotalLabor;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getQuotationId() {
        return quotationId;
    }

    public void setQuotationId(UUID quotationId) {
        this.quotationId = quotationId;
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
}
