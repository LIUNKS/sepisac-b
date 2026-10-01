package com.sepisac.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class OpenPurchaseOrderDTO {

    private UUID purchaseOrderId;
    private String orderNumber;
    private String status;
    private int requestedQuantity;
    private OffsetDateTime orderDate;
    private String supplierName;

    public OpenPurchaseOrderDTO() {
    }

    public OpenPurchaseOrderDTO(UUID purchaseOrderId, String orderNumber, String status, int requestedQuantity, OffsetDateTime orderDate, String supplierName) {
        this.purchaseOrderId = purchaseOrderId;
        this.orderNumber = orderNumber;
        this.status = status;
        this.requestedQuantity = requestedQuantity;
        this.orderDate = orderDate;
        this.supplierName = supplierName;
    }

    public UUID getPurchaseOrderId() {
        return purchaseOrderId;
    }

    public void setPurchaseOrderId(UUID purchaseOrderId) {
        this.purchaseOrderId = purchaseOrderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getRequestedQuantity() {
        return requestedQuantity;
    }

    public void setRequestedQuantity(int requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
    }

    public OffsetDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(OffsetDateTime orderDate) {
        this.orderDate = orderDate;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }
}
