package com.sepisac.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class QuotationLaborUpdateDTO {

    @NotBlank(message = "La especialidad requerida es obligatoria")
    @Size(max = 100, message = "La especialidad no puede exceder 100 caracteres")
    private String specialtyNeeded;

    @NotNull(message = "La cantidad de personal requerida es obligatoria")
    @Min(value = 1, message = "La cantidad requerida debe ser al menos 1")
    private Integer quantityRequired;

    @NotNull(message = "Las horas estimadas son obligatorias")
    @Min(value = 1, message = "Las horas estimadas deben ser al menos 1")
    private Integer estimatedHours;

    @NotNull(message = "El costo por hora es obligatorio")
    @PositiveOrZero(message = "El costo por hora no puede ser negativo")
    private BigDecimal lockedHourlyCost;

    public QuotationLaborUpdateDTO() {
    }

    public QuotationLaborUpdateDTO(String specialtyNeeded, Integer quantityRequired, Integer estimatedHours, BigDecimal lockedHourlyCost) {
        this.specialtyNeeded = specialtyNeeded;
        this.quantityRequired = quantityRequired;
        this.estimatedHours = estimatedHours;
        this.lockedHourlyCost = lockedHourlyCost;
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
}
