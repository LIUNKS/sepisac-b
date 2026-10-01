package com.sepisac.backend.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class PurchaseOrderDetailResponseDTO {

    private UUID id;
    private UUID inventoryItemId;
    private String itemSku;
    private String itemName;
    private Integer quantity;
    private BigDecimal unitCost;
    private BigDecimal subtotal;

    public PurchaseOrderDetailResponseDTO() {
    }

    public PurchaseOrderDetailResponseDTO(UUID id, UUID inventoryItemId, String itemSku, String itemName, Integer quantity, BigDecimal unitCost, BigDecimal subtotal) {
        this.id = id;
        this.inventoryItemId = inventoryItemId;
        this.itemSku = itemSku;
        this.itemName = itemName;
        this.quantity = quantity;
        this.unitCost = unitCost;
        this.subtotal = subtotal;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
}
