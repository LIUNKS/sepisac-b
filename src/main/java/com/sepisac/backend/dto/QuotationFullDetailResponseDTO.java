package com.sepisac.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class QuotationFullDetailResponseDTO {

    private UUID id;
    private UUID companyId;
    private String companyName;
    private String quotationNumber;
    private String clientName;
    private String serviceType;
    private String currency;
    private BigDecimal exchangeRate;
    private BigDecimal subtotalCosts;
    private BigDecimal profitMarginPercentage;
    private BigDecimal totalAmount;
    private String status;
    private OffsetDateTime createdAt;
    private List<QuotationDetailResponseDTO> details = new ArrayList<>();
    private List<QuotationLaborResponseDTO> laborRequirements = new ArrayList<>();

    public QuotationFullDetailResponseDTO() {
    }

    public QuotationFullDetailResponseDTO(UUID id, UUID companyId, String companyName, String quotationNumber,
                                          String clientName, String serviceType, String currency, BigDecimal exchangeRate,
                                          BigDecimal subtotalCosts, BigDecimal profitMarginPercentage, BigDecimal totalAmount,
                                          String status, OffsetDateTime createdAt, List<QuotationDetailResponseDTO> details,
                                          List<QuotationLaborResponseDTO> laborRequirements) {
        this.id = id;
        this.companyId = companyId;
        this.companyName = companyName;
        this.quotationNumber = quotationNumber;
        this.clientName = clientName;
        this.serviceType = serviceType;
        this.currency = currency;
        this.exchangeRate = exchangeRate;
        this.subtotalCosts = subtotalCosts;
        this.profitMarginPercentage = profitMarginPercentage;
        this.totalAmount = totalAmount;
        this.status = status;
        this.createdAt = createdAt;
        this.details = details != null ? details : new ArrayList<>();
        this.laborRequirements = laborRequirements != null ? laborRequirements : new ArrayList<>();
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

    public String getQuotationNumber() {
        return quotationNumber;
    }

    public void setQuotationNumber(String quotationNumber) {
        this.quotationNumber = quotationNumber;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public BigDecimal getSubtotalCosts() {
        return subtotalCosts;
    }

    public void setSubtotalCosts(BigDecimal subtotalCosts) {
        this.subtotalCosts = subtotalCosts;
    }

    public BigDecimal getProfitMarginPercentage() {
        return profitMarginPercentage;
    }

    public void setProfitMarginPercentage(BigDecimal profitMarginPercentage) {
        this.profitMarginPercentage = profitMarginPercentage;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<QuotationDetailResponseDTO> getDetails() {
        return details;
    }

    public void setDetails(List<QuotationDetailResponseDTO> details) {
        this.details = details;
    }

    public List<QuotationLaborResponseDTO> getLaborRequirements() {
        return laborRequirements;
    }

    public void setLaborRequirements(List<QuotationLaborResponseDTO> laborRequirements) {
        this.laborRequirements = laborRequirements;
    }
}
