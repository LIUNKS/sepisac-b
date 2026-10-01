package com.sepisac.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "DTO para asignar un empleado o técnico a un proyecto")
public class ProjectAssignmentCreateDTO {

    @NotNull(message = "El ID del empleado es obligatorio")
    @Schema(description = "ID del empleado a asignar")
    private UUID employeeId;

    @NotBlank(message = "El rol asignado es obligatorio")
    @Schema(description = "Rol o función asignada en el proyecto", example = "Técnico Especialista Eléctrico")
    private String assignedRole;

    @Schema(description = "Fecha de asignación (por defecto fecha actual)", example = "2026-10-01")
    private LocalDate assignedDate;

    public ProjectAssignmentCreateDTO() {
    }

    public ProjectAssignmentCreateDTO(UUID employeeId, String assignedRole, LocalDate assignedDate) {
        this.employeeId = employeeId;
        this.assignedRole = assignedRole;
        this.assignedDate = assignedDate;
    }

    public UUID getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(UUID employeeId) {
        this.employeeId = employeeId;
    }

    public String getAssignedRole() {
        return assignedRole;
    }

    public void setAssignedRole(String assignedRole) {
        this.assignedRole = assignedRole;
    }

    public LocalDate getAssignedDate() {
        return assignedDate;
    }

    public void setAssignedDate(LocalDate assignedDate) {
        this.assignedDate = assignedDate;
    }
}
