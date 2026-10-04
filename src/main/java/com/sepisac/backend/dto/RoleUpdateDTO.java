package com.sepisac.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para la actualización de un rol existente")
public class RoleUpdateDTO {

    @NotBlank(message = "El nombre del rol es obligatorio")
    @Size(max = 50, message = "El nombre del rol no debe superar los 50 caracteres")
    @Schema(description = "Nombre único del rol", example = "AUDITOR_ACTUALIZADO")
    private String name;

    @Size(max = 255, message = "La descripción no debe superar los 255 caracteres")
    @Schema(description = "Descripción detallada del rol", example = "Auditor de procesos y finanzas actualizado")
    private String description;

    public RoleUpdateDTO() {
    }

    public RoleUpdateDTO(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
