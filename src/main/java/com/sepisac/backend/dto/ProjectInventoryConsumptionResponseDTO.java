package com.sepisac.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "DTO de respuesta para consumo de inventario en proyecto")
public class ProjectInventoryConsumptionResponseDTO {

    @Schema(description = "ID del consumo")
    private UUID id;

    @Schema(description = "ID del proyecto")
    private UUID projectId;

    @Schema(description = "ID del ítem de inventario")
    private UUID inventoryItemId;

    @Schema(description = "SKU del material")
    private String itemSku;

    @Schema(description = "Nombre del material")
    private String itemName;

    @Schema(description = "Cantidad consumida")
    private Integer quantityConsumed;

    @Schema(description = "Stock restante actualizado en almacén")
    private Integer remainingStock;

    @Schema(description = "Fecha y hora del consumo")
    private OffsetDateTime consumptionDate;

    public ProjectInventoryConsumptionResponseDTO() {
    }

    public ProjectInventoryConsumptionResponseDTO(UUID id, UUID projectId, UUID inventoryItemId, String itemSku, String itemName, Integer quantityConsumed, Integer remainingStock, OffsetDateTime consumptionDate) {
        this.id = id;
        this.projectId = projectId;
        this.inventoryItemId = inventoryItemId;
        this.itemSku = itemSku;
        this.itemName = itemName;
        this.quantityConsumed = quantityConsumed;
        this.remainingStock = remainingStock;
        this.consumptionDate = consumptionDate;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public UUID getInventoryItemId() {
        return inventoryItemId;
    }

    public void setInventoryItemId(UUID inventoryItemId) {
        this.inventoryItemId = inventoryItemId;
    }

    public String getItemSku() {
        return itemSku;
    }

    public void setItemSku(String itemSku) {
        this.itemSku = itemSku;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Integer getQuantityConsumed() {
        return quantityConsumed;
    }

    public void setQuantityConsumed(Integer quantityConsumed) {
        this.quantityConsumed = quantityConsumed;
    }

    public Integer getRemainingStock() {
        return remainingStock;
    }

    public void setRemainingStock(Integer remainingStock) {
        this.remainingStock = remainingStock;
    }

    public OffsetDateTime getConsumptionDate() {
        return consumptionDate;
    }

    public void setConsumptionDate(OffsetDateTime consumptionDate) {
        this.consumptionDate = consumptionDate;
    }
}
