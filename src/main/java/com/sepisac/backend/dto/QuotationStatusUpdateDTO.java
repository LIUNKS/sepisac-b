package com.sepisac.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class QuotationStatusUpdateDTO {

    @NotBlank(message = "El estado es obligatorio")
    @Pattern(regexp = "^(BORRADOR|ENVIADA|APROBADA|RECHAZADA)$", message = "Estado de cotización inválido. Debe ser BORRADOR, ENVIADA, APROBADA o RECHAZADA")
    private String status;

    public QuotationStatusUpdateDTO() {
    }

    public QuotationStatusUpdateDTO(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
