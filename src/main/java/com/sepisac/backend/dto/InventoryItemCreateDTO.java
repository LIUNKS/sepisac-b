package com.sepisac.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public class InventoryItemCreateDTO {

    private UUID companyId;

    @NotBlank(message = "El SKU es obligatorio")
    @Size(min = 2, max = 50, message = "El SKU debe tener entre 2 y 50 caracteres")
    private String sku;

    @NotBlank(message = "El nombre del producto es obligatorio")
    @Size(max = 150, message = "El nombre no puede exceder 150 caracteres")
    private String name;

    private String description;

    @NotNull(message = "La cantidad de stock inicial es obligatoria")
    @PositiveOrZero(message = "El stock inicial no puede ser negativo")
    private Integer stockQuantity;

    @PositiveOrZero(message = "El costo de compra no puede ser negativo")
    private BigDecimal purchaseCost;

    @PositiveOrZero(message = "El precio de venta no puede ser negativo")
    private BigDecimal salePrice;

    @PositiveOrZero(message = "El stock mínimo de alerta no puede ser negativo")
    private Integer minStockAlert;

    public InventoryItemCreateDTO() {
    }

    public InventoryItemCreateDTO(UUID companyId, String sku, String name, String description,
                                  Integer stockQuantity, BigDecimal purchaseCost, BigDecimal salePrice, Integer minStockAlert) {
        this.companyId = companyId;
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.stockQuantity = stockQuantity;
        this.purchaseCost = purchaseCost;
        this.salePrice = salePrice;
        this.minStockAlert = minStockAlert;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
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
}
