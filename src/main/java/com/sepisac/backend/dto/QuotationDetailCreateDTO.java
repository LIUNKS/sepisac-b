package com.sepisac.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class QuotationDetailCreateDTO {

    @NotBlank(message = "La descripción del ítem es obligatoria")
    @Size(max = 255, message = "La descripción no puede exceder 255 caracteres")
    private String itemDescription;

    @NotBlank(message = "El tipo de ítem es obligatorio")
    @Size(max = 30, message = "El tipo de ítem no puede exceder 30 caracteres")
    private String itemType;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer quantity;

    @NotNull(message = "El precio unitario es obligatorio")
    @PositiveOrZero(message = "El precio unitario no puede ser negativo")
    private BigDecimal unitPrice;

    public QuotationDetailCreateDTO() {
    }

    public QuotationDetailCreateDTO(String itemDescription, String itemType, Integer quantity, BigDecimal unitPrice) {
        this.itemDescription = itemDescription;
        this.itemType = itemType;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
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
}
