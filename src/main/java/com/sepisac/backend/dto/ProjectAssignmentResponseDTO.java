package com.sepisac.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "DTO de respuesta para asignación de empleado a proyecto")
public class ProjectAssignmentResponseDTO {

    @Schema(description = "ID de la asignación")
    private UUID id;

    @Schema(description = "ID del proyecto")
    private UUID projectId;

    @Schema(description = "ID del empleado")
    private UUID employeeId;

    @Schema(description = "Nombre completo del empleado")
    private String employeeName;

    @Schema(description = "Especialidad del empleado")
    private String specialty;

    @Schema(description = "Rol asignado")
    private String assignedRole;

    @Schema(description = "Fecha de asignación")
    private LocalDate assignedDate;

    @Schema(description = "Estado de actividad de la asignación")
    private Boolean isActive;

    public ProjectAssignmentResponseDTO() {
    }

    public ProjectAssignmentResponseDTO(UUID id, UUID projectId, UUID employeeId, String employeeName, String specialty, String assignedRole, LocalDate assignedDate, Boolean isActive) {
        this.id = id;
        this.projectId = projectId;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.specialty = specialty;
        this.assignedRole = assignedRole;
        this.assignedDate = assignedDate;
        this.isActive = isActive;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public UUID getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(UUID employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
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

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
