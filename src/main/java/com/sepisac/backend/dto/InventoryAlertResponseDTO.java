package com.sepisac.backend.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InventoryAlertResponseDTO {

    private UUID itemId;
    private String sku;
    private String name;
    private int stockQuantity;
    private int minStockAlert;
    private int deficit;
    private List<OpenPurchaseOrderDTO> openPurchaseOrders = new ArrayList<>();
    private int totalRequestedQuantity;
    private boolean hasOpenOrder;

    public InventoryAlertResponseDTO() {
    }

    public InventoryAlertResponseDTO(UUID itemId,
                                     String sku,
                                     String name,
                                     int stockQuantity,
                                     int minStockAlert,
                                     int deficit,
                                     List<OpenPurchaseOrderDTO> openPurchaseOrders,
                                     int totalRequestedQuantity,
                                     boolean hasOpenOrder) {
        this.itemId = itemId;
        this.sku = sku;
        this.name = name;
        this.stockQuantity = stockQuantity;
        this.minStockAlert = minStockAlert;
        this.deficit = deficit;
        this.openPurchaseOrders = openPurchaseOrders != null ? openPurchaseOrders : new ArrayList<>();
        this.totalRequestedQuantity = totalRequestedQuantity;
        this.hasOpenOrder = hasOpenOrder;
    }

    public UUID getItemId() {
        return itemId;
    }

    public void setItemId(UUID itemId) {
        this.itemId = itemId;
    }

    public UUID getId() {
        return itemId;
    }

    public void setId(UUID id) {
        this.itemId = id;
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

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public int getMinStockAlert() {
        return minStockAlert;
    }

    public void setMinStockAlert(int minStockAlert) {
        this.minStockAlert = minStockAlert;
    }

    public int getDeficit() {
        return deficit;
    }

    public void setDeficit(int deficit) {
        this.deficit = deficit;
    }

    public List<OpenPurchaseOrderDTO> getOpenPurchaseOrders() {
        return openPurchaseOrders;
    }

    public void setOpenPurchaseOrders(List<OpenPurchaseOrderDTO> openPurchaseOrders) {
        this.openPurchaseOrders = openPurchaseOrders;
    }

    public int getTotalRequestedQuantity() {
        return totalRequestedQuantity;
    }

    public void setTotalRequestedQuantity(int totalRequestedQuantity) {
        this.totalRequestedQuantity = totalRequestedQuantity;
    }

    public boolean isHasOpenOrder() {
        return hasOpenOrder;
    }

    public void setHasOpenOrder(boolean hasOpenOrder) {
        this.hasOpenOrder = hasOpenOrder;
    }
}
