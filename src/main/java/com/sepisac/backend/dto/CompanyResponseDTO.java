package com.sepisac.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class CompanyResponseDTO {

    private UUID id;
    private String businessName;
    private String ruc;
    private String subscriptionStatus;
    private OffsetDateTime createdAt;

    public CompanyResponseDTO() {
    }

    public CompanyResponseDTO(UUID id, String businessName, String ruc, String subscriptionStatus, OffsetDateTime createdAt) {
        this.id = id;
        this.businessName = businessName;
        this.ruc = ruc;
        this.subscriptionStatus = subscriptionStatus;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public String getSubscriptionStatus() {
        return subscriptionStatus;
    }

    public void setSubscriptionStatus(String subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
