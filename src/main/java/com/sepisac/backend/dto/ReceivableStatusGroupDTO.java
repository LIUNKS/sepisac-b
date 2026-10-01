package com.sepisac.backend.dto;

import java.math.BigDecimal;

public class ReceivableStatusGroupDTO {

    private String status;
    private String currency;
    private BigDecimal totalBalance;
    private int invoiceCount;

    public ReceivableStatusGroupDTO() {
    }

    public ReceivableStatusGroupDTO(String status, String currency, BigDecimal totalBalance, int invoiceCount) {
        this.status = status;
        this.currency = currency;
        this.totalBalance = totalBalance;
        this.invoiceCount = invoiceCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getTotalBalance() {
        return totalBalance;
    }

    public void setTotalBalance(BigDecimal totalBalance) {
        this.totalBalance = totalBalance;
    }

    public int getInvoiceCount() {
        return invoiceCount;
    }

    public void setInvoiceCount(int invoiceCount) {
        this.invoiceCount = invoiceCount;
    }
}
