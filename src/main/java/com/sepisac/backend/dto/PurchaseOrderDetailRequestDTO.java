package com.sepisac.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public class PurchaseOrderDetailRequestDTO {

    @NotNull(message = "El ID del ítem de inventario es obligatorio")
    private UUID inventoryItemId;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser mayor a 0")
    private Integer quantity;

    @NotNull(message = "El costo unitario es obligatorio")
    @DecimalMin(value = "0.00", message = "El costo unitario no puede ser negativo")
    private BigDecimal unitCost;

    public PurchaseOrderDetailRequestDTO() {
    }

    public PurchaseOrderDetailRequestDTO(UUID inventoryItemId, Integer quantity, BigDecimal unitCost) {
        this.inventoryItemId = inventoryItemId;
        this.quantity = quantity;
        this.unitCost = unitCost;
    }

    public UUID getInventoryItemId() {
        return inventoryItemId;
    }

    public void setInventoryItemId(UUID inventoryItemId) {
        this.inventoryItemId = inventoryItemId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }
}
