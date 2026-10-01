package com.sepisac.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "DTO de respuesta para asignación de maquinaria a proyecto")
public class ProjectMachineryAssignmentResponseDTO {

    @Schema(description = "ID de la asignación")
    private UUID id;

    @Schema(description = "ID del proyecto")
    private UUID projectId;

    @Schema(description = "ID de la maquinaria")
    private UUID machineryEquipmentId;

    @Schema(description = "Código de la maquinaria")
    private String machineryCode;

    @Schema(description = "Nombre de la maquinaria")
    private String machineryName;

    @Schema(description = "Fecha de asignación")
    private LocalDate assignedDate;

    @Schema(description = "Fecha de retorno")
    private LocalDate returnDate;

    @Schema(description = "Estado de la asignación (ej. EN_USO)")
    private String status;

    public ProjectMachineryAssignmentResponseDTO() {
    }

    public ProjectMachineryAssignmentResponseDTO(UUID id, UUID projectId, UUID machineryEquipmentId, String machineryCode, String machineryName, LocalDate assignedDate, LocalDate returnDate, String status) {
        this.id = id;
        this.projectId = projectId;
        this.machineryEquipmentId = machineryEquipmentId;
        this.machineryCode = machineryCode;
        this.machineryName = machineryName;
        this.assignedDate = assignedDate;
        this.returnDate = returnDate;
        this.status = status;
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

    public UUID getMachineryEquipmentId() {
        return machineryEquipmentId;
    }

    public void setMachineryEquipmentId(UUID machineryEquipmentId) {
        this.machineryEquipmentId = machineryEquipmentId;
    }

    public String getMachineryCode() {
        return machineryCode;
    }

    public void setMachineryCode(String machineryCode) {
        this.machineryCode = machineryCode;
    }

    public String getMachineryName() {
        return machineryName;
    }

    public void setMachineryName(String machineryName) {
        this.machineryName = machineryName;
    }

    public LocalDate getAssignedDate() {
        return assignedDate;
    }

    public void setAssignedDate(LocalDate assignedDate) {
        this.assignedDate = assignedDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
