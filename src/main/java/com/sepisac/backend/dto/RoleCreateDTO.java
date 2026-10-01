package com.sepisac.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para la creación de un nuevo rol en el sistema")
public class RoleCreateDTO {

    @NotBlank(message = "El nombre del rol es obligatorio")
    @Size(max = 50, message = "El nombre del rol no debe superar los 50 caracteres")
    @Schema(description = "Nombre único del rol", example = "AUDITOR")
    private String name;

    @Schema(description = "Descripción detallada de los permisos o propósito del rol", example = "Auditor de procesos y finanzas")
    private String description;

    public RoleCreateDTO() {
    }

    public RoleCreateDTO(String name, String description) {
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
