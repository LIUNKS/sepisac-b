package com.sepisac.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public class EmployeeUpdateDTO {

    private UUID userId;

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 150, message = "El nombre no puede exceder 150 caracteres")
    private String fullName;

    @NotBlank(message = "La especialidad es obligatoria")
    @Size(max = 100, message = "La especialidad no puede exceder 100 caracteres")
    private String specialty;

    @NotBlank(message = "El tipo de contrato es obligatorio")
    @Pattern(regexp = "^(PLANILLA|LOCACION)$", message = "El tipo de contrato debe ser PLANILLA o LOCACION")
    private String contractType;

    @NotNull(message = "El sueldo base es obligatorio")
    @PositiveOrZero(message = "El sueldo base no puede ser negativo")
    private BigDecimal baseSalary;

    @NotNull(message = "El costo por hora es obligatorio")
    @PositiveOrZero(message = "El costo por hora no puede ser negativo")
    private BigDecimal currentHourlyCost;

    public EmployeeUpdateDTO() {
    }

    public EmployeeUpdateDTO(UUID userId, String fullName, String specialty,
                             String contractType, BigDecimal baseSalary, BigDecimal currentHourlyCost) {
        this.userId = userId;
        this.fullName = fullName;
        this.specialty = specialty;
        this.contractType = contractType;
        this.baseSalary = baseSalary;
        this.currentHourlyCost = currentHourlyCost;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public String getContractType() {
        return contractType;
    }

    public void setContractType(String contractType) {
        this.contractType = contractType;
    }

    public BigDecimal getBaseSalary() {
        return baseSalary;
    }

    public void setBaseSalary(BigDecimal baseSalary) {
        this.baseSalary = baseSalary;
    }

    public BigDecimal getCurrentHourlyCost() {
        return currentHourlyCost;
    }

    public void setCurrentHourlyCost(BigDecimal currentHourlyCost) {
        this.currentHourlyCost = currentHourlyCost;
    }
}
