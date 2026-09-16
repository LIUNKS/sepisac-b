package com.sepisac.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class SupplierResponseDTO {

    private UUID id;
    private UUID companyId;
    private String companyName;
    private String businessName;
    private String ruc;
    private String contactPhone;
    private String email;
    private OffsetDateTime createdAt;

    public SupplierResponseDTO() {
    }

    public SupplierResponseDTO(UUID id, UUID companyId, String companyName, String businessName,
                               String ruc, String contactPhone, String email, OffsetDateTime createdAt) {
        this.id = id;
        this.companyId = companyId;
        this.companyName = companyName;
        this.businessName = businessName;
        this.ruc = ruc;
        this.contactPhone = contactPhone;
        this.email = email;
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

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getRuc() {
        return ruc;
    }

    public void setRuc(String ruc) {
        this.ruc = ruc;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
