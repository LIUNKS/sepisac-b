package com.sepisac.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "DTO de respuesta para registro de pago o abono")
public class InvoicePaymentResponseDTO {

    @Schema(description = "ID del pago registrado")
    private UUID id;

    @Schema(description = "ID de la factura asociada")
    private UUID invoiceId;

    @Schema(description = "Número de la factura")
    private String invoiceNumber;

    @Schema(description = "Monto pagado o abonado", example = "400.00")
    private BigDecimal amountPaid;

    @Schema(description = "Método de pago utilizado", example = "TRANSFERENCIA")
    private String paymentMethod;

    @Schema(description = "Código de referencia bancaria", example = "OP-123456")
    private String referenceCode;

    @Schema(description = "Fecha y hora del pago")
    private OffsetDateTime paymentDate;

    @Schema(description = "Estado resultante de la factura tras el pago", example = "PARCIAL", allowableValues = {"PARCIAL", "PAGADA"})
    private String resultingPaymentStatus;

    @Schema(description = "Saldo restante por cobrar", example = "600.00")
    private BigDecimal remainingBalance;

    public InvoicePaymentResponseDTO() {
    }

    public InvoicePaymentResponseDTO(UUID id, UUID invoiceId, String invoiceNumber, BigDecimal amountPaid, String paymentMethod, String referenceCode, OffsetDateTime paymentDate, String resultingPaymentStatus, BigDecimal remainingBalance) {
        this.id = id;
        this.invoiceId = invoiceId;
        this.invoiceNumber = invoiceNumber;
        this.amountPaid = amountPaid;
        this.paymentMethod = paymentMethod;
        this.referenceCode = referenceCode;
        this.paymentDate = paymentDate;
        this.resultingPaymentStatus = resultingPaymentStatus;
        this.remainingBalance = remainingBalance;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(UUID invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getReferenceCode() {
        return referenceCode;
    }

    public void setReferenceCode(String referenceCode) {
        this.referenceCode = referenceCode;
    }

    public OffsetDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(OffsetDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getResultingPaymentStatus() {
        return resultingPaymentStatus;
    }

    public void setResultingPaymentStatus(String resultingPaymentStatus) {
        this.resultingPaymentStatus = resultingPaymentStatus;
    }

    public BigDecimal getRemainingBalance() {
        return remainingBalance;
    }

    public void setRemainingBalance(BigDecimal remainingBalance) {
        this.remainingBalance = remainingBalance;
    }
}
