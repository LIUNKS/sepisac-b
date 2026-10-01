package com.sepisac.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "DTO para actualizar el estado de un proyecto")
public class ProjectStatusUpdateDTO {

    @NotBlank(message = "El estado es obligatorio")
    @Pattern(regexp = "^(PENDIENTE|EN_PROCESO|COMPLETADO|CANCELADO)$", 
             message = "El estado debe ser PENDIENTE, EN_PROCESO, COMPLETADO o CANCELADO")
    @Schema(description = "Nuevo estado del proyecto", example = "EN_PROCESO", allowableValues = {"PENDIENTE", "EN_PROCESO", "COMPLETADO", "CANCELADO"})
    private String status;

    public ProjectStatusUpdateDTO() {
    }

    public ProjectStatusUpdateDTO(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
