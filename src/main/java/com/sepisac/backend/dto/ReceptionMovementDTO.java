package com.sepisac.backend.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class ReceptionMovementDTO {

    private UUID movementId;
    private UUID inventoryItemId;
    private String itemSku;
    private String itemName;
    private Integer quantityAdded;
    private Integer previousStock;
    private Integer newStock;
    private BigDecimal previousCost;
    private BigDecimal newCost;

    public ReceptionMovementDTO() {
    }

    public ReceptionMovementDTO(UUID movementId, UUID inventoryItemId, String itemSku, String itemName,
                                Integer quantityAdded, Integer previousStock, Integer newStock,
                                BigDecimal previousCost, BigDecimal newCost) {
        this.movementId = movementId;
        this.inventoryItemId = inventoryItemId;
        this.itemSku = itemSku;
        this.itemName = itemName;
        this.quantityAdded = quantityAdded;
        this.previousStock = previousStock;
        this.newStock = newStock;
        this.previousCost = previousCost;
        this.newCost = newCost;
    }

    public UUID getMovementId() {
        return movementId;
    }

    public void setMovementId(UUID movementId) {
        this.movementId = movementId;
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

    public Integer getQuantityAdded() {
        return quantityAdded;
    }

    public void setQuantityAdded(Integer quantityAdded) {
        this.quantityAdded = quantityAdded;
    }

    public Integer getPreviousStock() {
        return previousStock;
    }

    public void setPreviousStock(Integer previousStock) {
        this.previousStock = previousStock;
    }

    public Integer getNewStock() {
        return newStock;
    }

    public void setNewStock(Integer newStock) {
        this.newStock = newStock;
    }

    public BigDecimal getPreviousCost() {
        return previousCost;
    }

    public void setPreviousCost(BigDecimal previousCost) {
        this.previousCost = previousCost;
    }

    public BigDecimal getNewCost() {
        return newCost;
    }

    public void setNewCost(BigDecimal newCost) {
        this.newCost = newCost;
    }
}
