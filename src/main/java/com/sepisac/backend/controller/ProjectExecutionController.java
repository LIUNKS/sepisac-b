package com.sepisac.backend.controller;

import com.sepisac.backend.dto.*;
import com.sepisac.backend.security.UserPrincipal;
import com.sepisac.backend.service.ProjectExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/projects", "/api/projects"})
@Tag(name = "Project Execution & Resources", description = "Endpoints para la ejecución operativa de recursos: asignación de técnicos, maquinaria y consumo real de inventario")
@SecurityRequirement(name = "bearerAuth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ProjectExecutionController {

    private final ProjectExecutionService projectExecutionService;

    @Autowired
    public ProjectExecutionController(ProjectExecutionService projectExecutionService) {
        this.projectExecutionService = projectExecutionService;
    }

    @GetMapping("/{id}/inventory-consumptions")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA', 'ALMACEN', 'TECNICO')")
    @Operation(summary = "Listar consumos de inventario del proyecto", 
               description = "Obtiene la lista de todos los materiales consumidos en la ejecución del proyecto.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de consumos obtenida exitosamente"),
            @ApiResponse(responseCode = "404", description = "Proyecto no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<List<ProjectInventoryConsumptionResponseDTO>> getInventoryConsumptions(
            @Parameter(description = "ID del proyecto") @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<ProjectInventoryConsumptionResponseDTO> consumptions = projectExecutionService.getInventoryConsumptions(id, currentUser);
        return ResponseEntity.ok(consumptions);
    }

    @PostMapping("/{id}/inventory-consumptions")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA', 'ALMACEN', 'TECNICO')")
    @Operation(summary = "Registrar consumo real de inventario en obra", 
               description = "Registra la salida real de un material consumido en el proyecto, descuenta el stock en inventario y genera un movimiento de almacén SALIDA con auditoría.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Consumo registrado y stock descontado exitosamente", content = @Content(schema = @Schema(implementation = ProjectInventoryConsumptionResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Proyecto o material de inventario no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Stock insuficiente o conflicto de concurrencia", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<ProjectInventoryConsumptionResponseDTO> consumeInventory(
            @Parameter(description = "ID del proyecto") @PathVariable UUID id,
            @Valid @RequestBody ProjectInventoryConsumptionCreateDTO dto,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        ProjectInventoryConsumptionResponseDTO response = projectExecutionService.consumeInventory(id, dto, currentUser);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}/machinery-assignments")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA', 'ALMACEN', 'TECNICO')")
    @Operation(summary = "Listar maquinarias asignadas al proyecto", 
               description = "Obtiene la lista de maquinarias y equipos asignados al proyecto.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de maquinarias asignadas obtenida exitosamente"),
            @ApiResponse(responseCode = "404", description = "Proyecto no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<List<ProjectMachineryAssignmentResponseDTO>> getMachineryAssignments(
            @Parameter(description = "ID del proyecto") @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<ProjectMachineryAssignmentResponseDTO> assignments = projectExecutionService.getMachineryAssignments(id, currentUser);
        return ResponseEntity.ok(assignments);
    }

    @PostMapping("/{id}/machinery-assignments")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA', 'ALMACEN', 'TECNICO')")
    @Operation(summary = "Asignar maquinaria o equipo a proyecto", 
               description = "Asigna una maquinaria al proyecto y cambia su estado operativo global a EN_USO. Valida que no esté en mantenimiento ni en uso.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Maquinaria asignada exitosamente", content = @Content(schema = @Schema(implementation = ProjectMachineryAssignmentResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "La maquinaria no está disponible (en mantenimiento o en uso) o datos inválidos", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Proyecto o maquinaria no encontrados", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<ProjectMachineryAssignmentResponseDTO> assignMachinery(
            @Parameter(description = "ID del proyecto") @PathVariable UUID id,
            @Valid @RequestBody ProjectMachineryAssignmentCreateDTO dto) {
        ProjectMachineryAssignmentResponseDTO response = projectExecutionService.assignMachinery(id, dto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}/machinery-assignments/{assignmentId}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA', 'ALMACEN')")
    @Operation(summary = "Liberar asignación de maquinaria", 
               description = "Libera la maquinaria asignada a un proyecto, cambiando su estado operativo nuevamente a DISPONIBLE.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Maquinaria liberada y asignación eliminada exitosamente"),
            @ApiResponse(responseCode = "400", description = "La asignación no corresponde al proyecto indicado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Proyecto o asignación no encontrados", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<Void> releaseMachineryAssignment(
            @Parameter(description = "ID del proyecto") @PathVariable UUID id,
            @Parameter(description = "ID de la asignación de maquinaria") @PathVariable UUID assignmentId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        projectExecutionService.releaseMachineryAssignment(id, assignmentId, currentUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/assignments")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA', 'ALMACEN', 'TECNICO')")
    @Operation(summary = "Listar empleados/personal asignados al proyecto", 
               description = "Obtiene la lista de empleados asignados a la cuadrilla del proyecto.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de empleados asignados obtenida exitosamente"),
            @ApiResponse(responseCode = "404", description = "Proyecto no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<List<ProjectAssignmentResponseDTO>> getEmployeeAssignments(
            @Parameter(description = "ID del proyecto") @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<ProjectAssignmentResponseDTO> assignments = projectExecutionService.getEmployeeAssignments(id, currentUser);
        return ResponseEntity.ok(assignments);
    }

    @PostMapping("/{id}/assignments")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA', 'ALMACEN', 'TECNICO')")
    @Operation(summary = "Asignar personal/técnico a proyecto", 
               description = "Asigna un empleado o técnico al proyecto especificando su rol asignado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Empleado asignado exitosamente", content = @Content(schema = @Schema(implementation = ProjectAssignmentResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos de asignación inválidos", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Proyecto o empleado no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<ProjectAssignmentResponseDTO> assignEmployee(
            @Parameter(description = "ID del proyecto") @PathVariable UUID id,
            @Valid @RequestBody ProjectAssignmentCreateDTO dto) {
        ProjectAssignmentResponseDTO response = projectExecutionService.assignEmployee(id, dto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}/assignments/{assignmentId}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA')")
    @Operation(summary = "Desasignar empleado del proyecto", 
               description = "Elimina la asignación de un empleado o técnico de la cuadrilla del proyecto.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Asignación eliminada exitosamente"),
            @ApiResponse(responseCode = "400", description = "La asignación no corresponde al proyecto indicado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Proyecto o asignación no encontrados", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<Void> removeEmployeeAssignment(
            @Parameter(description = "ID del proyecto") @PathVariable UUID id,
            @Parameter(description = "ID de la asignación del empleado") @PathVariable UUID assignmentId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        projectExecutionService.removeEmployeeAssignment(id, assignmentId, currentUser);
        return ResponseEntity.noContent().build();
    }
}
