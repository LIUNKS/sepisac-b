package com.sepisac.backend.dto;

import java.math.BigDecimal;

public class CurrencyTotalDTO {

    private String currency;
    private BigDecimal totalAmount;
    private int paymentCount;

    public CurrencyTotalDTO() {
    }

    public CurrencyTotalDTO(String currency, BigDecimal totalAmount) {
        this.currency = currency;
        this.totalAmount = totalAmount;
    }

    public CurrencyTotalDTO(String currency, BigDecimal totalAmount, int paymentCount) {
        this.currency = currency;
        this.totalAmount = totalAmount;
        this.paymentCount = paymentCount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public int getPaymentCount() {
        return paymentCount;
    }

    public void setPaymentCount(int paymentCount) {
        this.paymentCount = paymentCount;
    }
}
