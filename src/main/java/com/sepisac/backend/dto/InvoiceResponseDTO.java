package com.sepisac.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "DTO de respuesta con información completa de una factura comercial")
public class InvoiceResponseDTO {

    @Schema(description = "ID único de la factura")
    private UUID id;

    @Schema(description = "ID de la empresa tenant")
    private UUID companyId;

    @Schema(description = "Razón social de la empresa")
    private String companyName;

    @Schema(description = "ID de la cotización origen")
    private UUID quotationId;

    @Schema(description = "Número de la cotización origen")
    private String quotationNumber;

    @Schema(description = "ID del proyecto asociado (opcional)")
    private UUID projectId;

    @Schema(description = "Código del proyecto asociado (opcional)")
    private String projectCode;

    @Schema(description = "Nombre o razón social del cliente")
    private String clientName;

    @Schema(description = "Número legal de la factura", example = "FAC-2026-001")
    private String invoiceNumber;

    @Schema(description = "Moneda de la factura", example = "PEN")
    private String currency;

    @Schema(description = "Monto total facturado", example = "1000.00")
    private BigDecimal totalAmount;

    @Schema(description = "Total pagado o abonado acumulado", example = "400.00")
    private BigDecimal totalPaid;

    @Schema(description = "Saldo deudor pendiente", example = "600.00")
    private BigDecimal balanceDue;

    @Schema(description = "Estado de cobranza", example = "PENDIENTE", allowableValues = {"PENDIENTE", "PARCIAL", "PAGADA", "VENCIDA", "ANULADA"})
    private String paymentStatus;

    @Schema(description = "Fecha de emisión", example = "2026-10-01")
    private LocalDate issueDate;

    @Schema(description = "Fecha de vencimiento", example = "2026-10-31")
    private LocalDate dueDate;

    @Schema(description = "Fecha y hora de creación")
    private OffsetDateTime createdAt;

    public InvoiceResponseDTO() {
    }

    public InvoiceResponseDTO(UUID id, UUID companyId, String companyName, UUID quotationId, String quotationNumber,
                              UUID projectId, String projectCode, String clientName, String invoiceNumber,
                              String currency, BigDecimal totalAmount, BigDecimal totalPaid, BigDecimal balanceDue,
                              String paymentStatus, LocalDate issueDate, LocalDate dueDate, OffsetDateTime createdAt) {
        this.id = id;
        this.companyId = companyId;
        this.companyName = companyName;
        this.quotationId = quotationId;
        this.quotationNumber = quotationNumber;
        this.projectId = projectId;
        this.projectCode = projectCode;
        this.clientName = clientName;
        this.invoiceNumber = invoiceNumber;
        this.currency = currency;
        this.totalAmount = totalAmount;
        this.totalPaid = totalPaid;
        this.balanceDue = balanceDue;
        this.paymentStatus = paymentStatus;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public UUID getQuotationId() {
        return quotationId;
    }

    public void setQuotationId(UUID quotationId) {
        this.quotationId = quotationId;
    }

    public String getQuotationNumber() {
        return quotationNumber;
    }

    public void setQuotationNumber(String quotationNumber) {
        this.quotationNumber = quotationNumber;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public void setProjectCode(String projectCode) {
        this.projectCode = projectCode;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
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

    public BigDecimal getTotalPaid() {
        return totalPaid;
    }

    public void setTotalPaid(BigDecimal totalPaid) {
        this.totalPaid = totalPaid;
    }

    public BigDecimal getBalanceDue() {
        return balanceDue;
    }

    public void setBalanceDue(BigDecimal balanceDue) {
        this.balanceDue = balanceDue;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
