package com.sepisac.backend.dto;

public class AutoGenerateOrdersResponseDTO {

    private int ordersCreated;
    private int itemsWithoutSupplier;
    private int itemsSkippedWithOpenOrder;

    public AutoGenerateOrdersResponseDTO() {
    }

    public AutoGenerateOrdersResponseDTO(int ordersCreated, int itemsWithoutSupplier, int itemsSkippedWithOpenOrder) {
        this.ordersCreated = ordersCreated;
        this.itemsWithoutSupplier = itemsWithoutSupplier;
        this.itemsSkippedWithOpenOrder = itemsSkippedWithOpenOrder;
    }

    public int getOrdersCreated() {
        return ordersCreated;
    }

    public void setOrdersCreated(int ordersCreated) {
        this.ordersCreated = ordersCreated;
    }

    public int getItemsWithoutSupplier() {
        return itemsWithoutSupplier;
    }

    public void setItemsWithoutSupplier(int itemsWithoutSupplier) {
        this.itemsWithoutSupplier = itemsWithoutSupplier;
    }

    public int getItemsSkippedWithOpenOrder() {
        return itemsSkippedWithOpenOrder;
    }

    public void setItemsSkippedWithOpenOrder(int itemsSkippedWithOpenOrder) {
        this.itemsSkippedWithOpenOrder = itemsSkippedWithOpenOrder;
    }
}
