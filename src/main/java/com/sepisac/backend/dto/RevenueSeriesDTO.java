package com.sepisac.backend.dto;

import java.math.BigDecimal;

public class RevenueSeriesDTO {

    private String period;
    private String currency;
    private BigDecimal totalAmount;
    private int paymentCount;

    public RevenueSeriesDTO() {
    }

    public RevenueSeriesDTO(String period, String currency, BigDecimal totalAmount, int paymentCount) {
        this.period = period;
        this.currency = currency;
        this.totalAmount = totalAmount;
        this.paymentCount = paymentCount;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
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
