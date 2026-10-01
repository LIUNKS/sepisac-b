package com.sepisac.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class UpdatePurchaseOrderStatusRequestDTO {

    @NotBlank(message = "El estado es obligatorio")
    @Pattern(regexp = "^(?i)CANCELADA$", message = "Solo se permite cambiar el estado a CANCELADA")
    private String status;

    public UpdatePurchaseOrderStatusRequestDTO() {
    }

    public UpdatePurchaseOrderStatusRequestDTO(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
