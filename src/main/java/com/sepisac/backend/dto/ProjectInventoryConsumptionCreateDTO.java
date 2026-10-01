package com.sepisac.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

@Schema(description = "DTO para registrar el consumo real de inventario en un proyecto")
public class ProjectInventoryConsumptionCreateDTO {

    @NotNull(message = "El ID del ítem de inventario es obligatorio")
    @Schema(description = "ID del ítem de inventario")
    private UUID inventoryItemId;

    @NotNull(message = "La cantidad consumida es obligatoria")
    @Positive(message = "La cantidad consumida debe ser mayor a cero")
    @Schema(description = "Cantidad a consumir de inventario", example = "5")
    private Integer quantity;

    @Schema(description = "Motivo o detalle del consumo (opcional)", example = "Tuberías para tendido eléctrico tramo 1")
    private String reason;

    public ProjectInventoryConsumptionCreateDTO() {
    }

    public ProjectInventoryConsumptionCreateDTO(UUID inventoryItemId, Integer quantity, String reason) {
        this.inventoryItemId = inventoryItemId;
        this.quantity = quantity;
        this.reason = reason;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
