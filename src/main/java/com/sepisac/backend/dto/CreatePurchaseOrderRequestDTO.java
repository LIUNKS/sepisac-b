package com.sepisac.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class CreatePurchaseOrderRequestDTO {

    @NotNull(message = "El proveedor es obligatorio")
    private UUID supplierId;

    private String currency = "PEN";

    private BigDecimal exchangeRate = new BigDecimal("1.0000");

    @NotEmpty(message = "La orden debe contener al menos un ítem en los detalles")
    @Valid
    private List<PurchaseOrderDetailRequestDTO> details;

    public CreatePurchaseOrderRequestDTO() {
    }

    public CreatePurchaseOrderRequestDTO(UUID supplierId, String currency, BigDecimal exchangeRate, List<PurchaseOrderDetailRequestDTO> details) {
        this.supplierId = supplierId;
        this.currency = currency;
        this.exchangeRate = exchangeRate;
        this.details = details;
    }

    public UUID getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(UUID supplierId) {
        this.supplierId = supplierId;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public List<PurchaseOrderDetailRequestDTO> getDetails() {
        return details;
    }

    public void setDetails(List<PurchaseOrderDetailRequestDTO> details) {
        this.details = details;
    }
}
