package com.sepisac.backend.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class QuotationDetailResponseDTO {

    private UUID id;
    private UUID quotationId;
    private String itemDescription;
    private String itemType;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;

    public QuotationDetailResponseDTO() {
    }

    public QuotationDetailResponseDTO(UUID id, UUID quotationId, String itemDescription, String itemType,
                                      Integer quantity, BigDecimal unitPrice, BigDecimal subtotal) {
        this.id = id;
        this.quotationId = quotationId;
        this.itemDescription = itemDescription;
        this.itemType = itemType;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = subtotal;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getQuotationId() {
        return quotationId;
    }

    public void setQuotationId(UUID quotationId) {
        this.quotationId = quotationId;
    }

    public String getItemDescription() {
        return itemDescription;
    }

    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
}
