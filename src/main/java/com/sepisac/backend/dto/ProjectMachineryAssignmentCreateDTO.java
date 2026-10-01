package com.sepisac.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "DTO para asignar una maquinaria o equipo a un proyecto")
public class ProjectMachineryAssignmentCreateDTO {

    @NotNull(message = "El ID de la maquinaria es obligatorio")
    @Schema(description = "ID del equipo o maquinaria")
    private UUID machineryEquipmentId;

    @Schema(description = "Fecha de asignación (por defecto fecha actual)", example = "2026-10-01")
    private LocalDate assignedDate;

    @Schema(description = "Fecha estimada o real de devolución", example = "2026-10-15")
    private LocalDate returnDate;

    public ProjectMachineryAssignmentCreateDTO() {
    }

    public ProjectMachineryAssignmentCreateDTO(UUID machineryEquipmentId, LocalDate assignedDate, LocalDate returnDate) {
        this.machineryEquipmentId = machineryEquipmentId;
        this.assignedDate = assignedDate;
        this.returnDate = returnDate;
    }

    public UUID getMachineryEquipmentId() {
        return machineryEquipmentId;
    }

    public void setMachineryEquipmentId(UUID machineryEquipmentId) {
        this.machineryEquipmentId = machineryEquipmentId;
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
}
