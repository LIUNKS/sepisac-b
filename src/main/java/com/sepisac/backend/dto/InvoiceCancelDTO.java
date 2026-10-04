package com.sepisac.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Datos para la anulación de factura")
public class InvoiceCancelDTO {

    @Schema(description = "Motivo de la anulación de la factura", example = "Error en emisión de cotización")
    private String reason;

    public InvoiceCancelDTO() {
    }

    public InvoiceCancelDTO(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
