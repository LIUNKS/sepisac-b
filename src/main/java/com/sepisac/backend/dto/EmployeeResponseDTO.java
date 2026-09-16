package com.sepisac.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class EmployeeResponseDTO {

    private UUID id;
    private UUID companyId;
    private String companyName;
    private UUID userId;
    private String userEmail;
    private String fullName;
    private String specialty;
    private String contractType;
    private BigDecimal baseSalary;
    private BigDecimal currentHourlyCost;
    private Boolean isAvailable;
    private OffsetDateTime createdAt;

    public EmployeeResponseDTO() {
    }

    public EmployeeResponseDTO(UUID id, UUID companyId, String companyName, UUID userId, String userEmail,
                               String fullName, String specialty, String contractType, BigDecimal baseSalary,
                               BigDecimal currentHourlyCost, Boolean isAvailable, OffsetDateTime createdAt) {
        this.id = id;
        this.companyId = companyId;
        this.companyName = companyName;
        this.userId = userId;
        this.userEmail = userEmail;
        this.fullName = fullName;
        this.specialty = specialty;
        this.contractType = contractType;
        this.baseSalary = baseSalary;
        this.currentHourlyCost = currentHourlyCost;
        this.isAvailable = isAvailable;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
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

    public Boolean getIsAvailable() {
        return isAvailable;
    }

    public void setIsAvailable(Boolean isAvailable) {
        this.isAvailable = isAvailable;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
