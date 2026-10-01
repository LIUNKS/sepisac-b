package com.sepisac.backend.dto;

import java.util.ArrayList;
import java.util.List;

public class ReceivablesKpiResponseDTO {

    private List<ReceivableStatusGroupDTO> statusGroups = new ArrayList<>();
    private PaidSummaryDTO paidSummary;

    public ReceivablesKpiResponseDTO() {
    }

    public ReceivablesKpiResponseDTO(List<ReceivableStatusGroupDTO> statusGroups, PaidSummaryDTO paidSummary) {
        this.statusGroups = statusGroups != null ? statusGroups : new ArrayList<>();
        this.paidSummary = paidSummary;
    }

    public List<ReceivableStatusGroupDTO> getStatusGroups() {
        return statusGroups;
    }

    public void setStatusGroups(List<ReceivableStatusGroupDTO> statusGroups) {
        this.statusGroups = statusGroups;
    }

    // Alias for flexibility in test or API consumption
    public List<ReceivableStatusGroupDTO> getReceivables() {
        return statusGroups;
    }

    public void setReceivables(List<ReceivableStatusGroupDTO> receivables) {
        this.statusGroups = receivables;
    }

    public PaidSummaryDTO getPaidSummary() {
        return paidSummary;
    }

    public void setPaidSummary(PaidSummaryDTO paidSummary) {
        this.paidSummary = paidSummary;
    }
}
