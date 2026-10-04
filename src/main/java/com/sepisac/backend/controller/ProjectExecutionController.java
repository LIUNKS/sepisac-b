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

import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/projects", "/api/projects"})
@Tag(name = "Project Execution & Resources", description = "Endpoints para la ejecución operativa de recursos: asignación de técnicos, maquinaria y consumo real de inventario")
@SecurityRequirement(name = "bearerAuth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ProjectExecutionController {

    @GetMapping("/{id}/inventory-consumptions")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA', 'ALMACEN', 'TECNICO')")
    @Operation(summary = "Listar consumos de inventario del proyecto")
    public ResponseEntity<java.util.List<ProjectInventoryConsumptionResponseDTO>> getInventoryConsumptions(@PathVariable UUID id) {
        return ResponseEntity.ok(projectExecutionService.getInventoryConsumptions(id));
    }

    @GetMapping("/{id}/machinery-assignments")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA', 'ALMACEN', 'TECNICO')")
    @Operation(summary = "Listar asignaciones de maquinaria del proyecto")
    public ResponseEntity<java.util.List<ProjectMachineryAssignmentResponseDTO>> getMachineryAssignments(@PathVariable UUID id) {
        return ResponseEntity.ok(projectExecutionService.getMachineryAssignments(id));
    }

    @GetMapping("/{id}/assignments")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA', 'ALMACEN', 'TECNICO')")
    @Operation(summary = "Listar asignaciones de personal del proyecto")
    public ResponseEntity<java.util.List<ProjectAssignmentResponseDTO>> getEmployeeAssignments(@PathVariable UUID id) {
        return ResponseEntity.ok(projectExecutionService.getEmployeeAssignments(id));
    }


    private final ProjectExecutionService projectExecutionService;

    @Autowired
    public ProjectExecutionController(ProjectExecutionService projectExecutionService) {
        this.projectExecutionService = projectExecutionService;
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
}
