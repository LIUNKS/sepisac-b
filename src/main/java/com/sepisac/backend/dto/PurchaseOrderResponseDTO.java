package com.sepisac.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PurchaseOrderResponseDTO {

    private UUID id;
    private UUID companyId;
    private UUID supplierId;
    private String supplierName;
    private String supplierRuc;
    private String orderNumber;
    private String currency;
    private BigDecimal exchangeRate;
    private String status;
    private BigDecimal totalAmount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<PurchaseOrderDetailResponseDTO> details = new ArrayList<>();

    public PurchaseOrderResponseDTO() {
    }

    public PurchaseOrderResponseDTO(UUID id, UUID companyId, UUID supplierId, String supplierName, String supplierRuc,
                                    String orderNumber, String currency, BigDecimal exchangeRate, String status,
                                    BigDecimal totalAmount, OffsetDateTime createdAt, OffsetDateTime updatedAt,
                                    List<PurchaseOrderDetailResponseDTO> details) {
        this.id = id;
        this.companyId = companyId;
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.supplierRuc = supplierRuc;
        this.orderNumber = orderNumber;
        this.currency = currency;
        this.exchangeRate = exchangeRate;
        this.status = status;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.details = details != null ? details : new ArrayList<>();
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

    public UUID getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(UUID supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getSupplierRuc() {
        return supplierRuc;
    }

    public void setSupplierRuc(String supplierRuc) {
        this.supplierRuc = supplierRuc;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<PurchaseOrderDetailResponseDTO> getDetails() {
        return details;
    }

    public void setDetails(List<PurchaseOrderDetailResponseDTO> details) {
        this.details = details != null ? details : new ArrayList<>();
    }
}
