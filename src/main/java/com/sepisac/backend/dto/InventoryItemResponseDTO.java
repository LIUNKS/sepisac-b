package com.sepisac.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class InventoryItemResponseDTO {

    private UUID id;
    private UUID companyId;
    private String companyName;
    private String sku;
    private String name;
    private String description;
    private Integer stockQuantity;
    private BigDecimal purchaseCost;
    private BigDecimal salePrice;
    private Integer minStockAlert;
    private Boolean isLowStock;
    private OffsetDateTime createdAt;

    public InventoryItemResponseDTO() {
    }

    public InventoryItemResponseDTO(UUID id, UUID companyId, String companyName, String sku, String name,
                                    String description, Integer stockQuantity, BigDecimal purchaseCost,
                                    BigDecimal salePrice, Integer minStockAlert, Boolean isLowStock,
                                    OffsetDateTime createdAt) {
        this.id = id;
        this.companyId = companyId;
        this.companyName = companyName;
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.stockQuantity = stockQuantity;
        this.purchaseCost = purchaseCost;
        this.salePrice = salePrice;
        this.minStockAlert = minStockAlert;
        this.isLowStock = isLowStock;
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

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public BigDecimal getPurchaseCost() {
        return purchaseCost;
    }

    public void setPurchaseCost(BigDecimal purchaseCost) {
        this.purchaseCost = purchaseCost;
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }

    public Integer getMinStockAlert() {
        return minStockAlert;
    }

    public void setMinStockAlert(Integer minStockAlert) {
        this.minStockAlert = minStockAlert;
    }

    public Boolean getIsLowStock() {
        return isLowStock;
    }

    public void setIsLowStock(Boolean isLowStock) {
        this.isLowStock = isLowStock;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
