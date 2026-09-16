package com.sepisac.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class MachineryStatusUpdateDTO {

    @NotBlank(message = "El estado es obligatorio")
    @Pattern(regexp = "^(DISPONIBLE|EN_USO|EN_MANTENIMIENTO|DE_BAJA)$", message = "Estado de maquinaria inválido. Debe ser DISPONIBLE, EN_USO, EN_MANTENIMIENTO o DE_BAJA")
    private String status;

    public MachineryStatusUpdateDTO() {
    }

    public MachineryStatusUpdateDTO(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
