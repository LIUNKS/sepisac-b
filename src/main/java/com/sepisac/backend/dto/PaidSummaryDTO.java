package com.sepisac.backend.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class PaidSummaryDTO {

    private int invoiceCount;
    private List<CurrencyTotalDTO> totalsByCurrency = new ArrayList<>();
    private BigDecimal totalPaid;
    private String currency;

    public PaidSummaryDTO() {
    }

    public PaidSummaryDTO(int invoiceCount, List<CurrencyTotalDTO> totalsByCurrency) {
        this.invoiceCount = invoiceCount;
        this.totalsByCurrency = totalsByCurrency != null ? totalsByCurrency : new ArrayList<>();
    }

    public PaidSummaryDTO(int invoiceCount, BigDecimal totalPaid, String currency) {
        this.invoiceCount = invoiceCount;
        this.totalPaid = totalPaid;
        this.currency = currency;
    }

    public PaidSummaryDTO(int invoiceCount, List<CurrencyTotalDTO> totalsByCurrency, BigDecimal totalPaid, String currency) {
        this.invoiceCount = invoiceCount;
        this.totalsByCurrency = totalsByCurrency != null ? totalsByCurrency : new ArrayList<>();
        this.totalPaid = totalPaid;
        this.currency = currency;
    }

    public int getInvoiceCount() {
        return invoiceCount;
    }

    public void setInvoiceCount(int invoiceCount) {
        this.invoiceCount = invoiceCount;
    }

    public List<CurrencyTotalDTO> getTotalsByCurrency() {
        return totalsByCurrency;
    }

    public void setTotalsByCurrency(List<CurrencyTotalDTO> totalsByCurrency) {
        this.totalsByCurrency = totalsByCurrency;
    }

    public BigDecimal getTotalPaid() {
        return totalPaid;
    }

    public void setTotalPaid(BigDecimal totalPaid) {
        this.totalPaid = totalPaid;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
