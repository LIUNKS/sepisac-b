package com.sepisac.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class InventoryReplenishmentSettingsRequestDTO {

    private UUID supplierId;

    @NotNull(message = "El stock mínimo de alerta es obligatorio")
    @Min(value = 0, message = "El stock mínimo de alerta no puede ser negativo")
    private Integer minStockAlert;

    @NotNull(message = "La cantidad de reorden es obligatoria")
    @Min(value = 1, message = "La cantidad de reorden debe ser mayor a 0")
    private Integer reorderQuantity;

    public InventoryReplenishmentSettingsRequestDTO() {
    }

    public InventoryReplenishmentSettingsRequestDTO(UUID supplierId, Integer minStockAlert, Integer reorderQuantity) {
        this.supplierId = supplierId;
        this.minStockAlert = minStockAlert;
        this.reorderQuantity = reorderQuantity;
    }

    public UUID getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(UUID supplierId) {
        this.supplierId = supplierId;
    }

    public Integer getMinStockAlert() {
        return minStockAlert;
    }

    public void setMinStockAlert(Integer minStockAlert) {
        this.minStockAlert = minStockAlert;
    }

    public Integer getReorderQuantity() {
        return reorderQuantity;
    }

    public void setReorderQuantity(Integer reorderQuantity) {
        this.reorderQuantity = reorderQuantity;
    }
}
