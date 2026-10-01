package com.sepisac.backend.dto;

public class CommercialCycleKpiResponseDTO {

    private StageKpiDTO creationToApproval;
    private StageKpiDTO approvalToProjectStart;
    private StageKpiDTO projectStartToInvoice;
    private TotalKpiDTO total;

    public CommercialCycleKpiResponseDTO() {
    }

    public CommercialCycleKpiResponseDTO(StageKpiDTO creationToApproval,
                                         StageKpiDTO approvalToProjectStart,
                                         StageKpiDTO projectStartToInvoice,
                                         TotalKpiDTO total) {
        this.creationToApproval = creationToApproval;
        this.approvalToProjectStart = approvalToProjectStart;
        this.projectStartToInvoice = projectStartToInvoice;
        this.total = total;
    }

    public StageKpiDTO getCreationToApproval() {
        return creationToApproval;
    }

    public void setCreationToApproval(StageKpiDTO creationToApproval) {
        this.creationToApproval = creationToApproval;
    }

    public StageKpiDTO getApprovalToProjectStart() {
        return approvalToProjectStart;
    }

    public void setApprovalToProjectStart(StageKpiDTO approvalToProjectStart) {
        this.approvalToProjectStart = approvalToProjectStart;
    }

    public StageKpiDTO getProjectStartToInvoice() {
        return projectStartToInvoice;
    }

    public void setProjectStartToInvoice(StageKpiDTO projectStartToInvoice) {
        this.projectStartToInvoice = projectStartToInvoice;
    }

    public TotalKpiDTO getTotal() {
        return total;
    }

    public void setTotal(TotalKpiDTO total) {
        this.total = total;
    }
}
