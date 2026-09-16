package com.sepisac.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class QuotationUpdateDTO {

    @NotBlank(message = "El nombre del cliente es obligatorio")
    @Size(max = 150, message = "El nombre del cliente no puede exceder 150 caracteres")
    private String clientName;

    @NotBlank(message = "El tipo de servicio es obligatorio")
    @Size(max = 50, message = "El tipo de servicio no puede exceder 50 caracteres")
    private String serviceType;

    @Pattern(regexp = "^(PEN|USD)$", message = "La moneda debe ser PEN o USD")
    private String currency;

    @PositiveOrZero(message = "El tipo de cambio no puede ser negativo")
    private BigDecimal exchangeRate;

    @NotNull(message = "El margen de ganancia es obligatorio")
    @PositiveOrZero(message = "El margen de ganancia no puede ser negativo")
    private BigDecimal profitMarginPercentage;

    public QuotationUpdateDTO() {
    }

    public QuotationUpdateDTO(String clientName, String serviceType, String currency,
                              BigDecimal exchangeRate, BigDecimal profitMarginPercentage) {
        this.clientName = clientName;
        this.serviceType = serviceType;
        this.currency = currency;
        this.exchangeRate = exchangeRate;
        this.profitMarginPercentage = profitMarginPercentage;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
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

    public BigDecimal getProfitMarginPercentage() {
        return profitMarginPercentage;
    }

    public void setProfitMarginPercentage(BigDecimal profitMarginPercentage) {
        this.profitMarginPercentage = profitMarginPercentage;
    }
}
