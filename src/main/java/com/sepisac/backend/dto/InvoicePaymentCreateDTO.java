package com.sepisac.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(description = "DTO para registrar un nuevo pago o abono a una factura")
public class InvoicePaymentCreateDTO {

    @NotNull(message = "El monto pagado es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto pagado debe ser mayor a 0")
    @Schema(description = "Monto del abono o pago", example = "400.00")
    private BigDecimal amountPaid;

    @NotBlank(message = "El método de pago es obligatorio")
    @Schema(description = "Método de pago utilizado", example = "TRANSFERENCIA", allowableValues = {"TRANSFERENCIA", "EFECTIVO", "DEPOSITO", "TARJETA", "CHEQUE"})
    private String paymentMethod;

    @Schema(description = "Código de referencia bancaria o número de operación", example = "OP-987654321")
    private String referenceCode;

    @Schema(description = "Moneda del pago (si no se envía, se asume la moneda de la factura)", example = "PEN")
    private String currency;

    @Schema(description = "Tipo de cambio aplicado en caso de conversión de moneda", example = "3.7500")
    private BigDecimal exchangeRate;

    @Schema(description = "Fecha y hora del pago (por defecto fecha actual)")
    private OffsetDateTime paymentDate;

    public InvoicePaymentCreateDTO() {
    }

    public InvoicePaymentCreateDTO(BigDecimal amountPaid, String paymentMethod, String referenceCode) {
        this.amountPaid = amountPaid;
        this.paymentMethod = paymentMethod;
        this.referenceCode = referenceCode;
    }

    public InvoicePaymentCreateDTO(BigDecimal amountPaid, String paymentMethod, String referenceCode, String currency, BigDecimal exchangeRate) {
        this.amountPaid = amountPaid;
        this.paymentMethod = paymentMethod;
        this.referenceCode = referenceCode;
        this.currency = currency;
        this.exchangeRate = exchangeRate;
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

    public OffsetDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(OffsetDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }
}
