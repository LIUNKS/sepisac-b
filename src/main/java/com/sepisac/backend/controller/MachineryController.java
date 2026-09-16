package com.sepisac.backend.controller;

import com.sepisac.backend.dto.ErrorResponseDTO;
import com.sepisac.backend.dto.MachineryCreateDTO;
import com.sepisac.backend.dto.MachineryFilterDTO;
import com.sepisac.backend.dto.MachineryResponseDTO;
import com.sepisac.backend.dto.MachineryStatusUpdateDTO;
import com.sepisac.backend.dto.MachineryUpdateDTO;
import com.sepisac.backend.dto.PageResponseDTO;
import com.sepisac.backend.security.UserPrincipal;
import com.sepisac.backend.service.MachineryEquipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/machinery")
@Tag(name = "Machinery", description = "Endpoints para la gestión de Activos, Maquinaria Operativa y Mantenimientos")
@SecurityRequirement(name = "bearerAuth")
public class MachineryController {

    private final MachineryEquipmentService machineryService;

    public MachineryController(MachineryEquipmentService machineryService) {
        this.machineryService = machineryService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA')")
    @Operation(summary = "Registrar nuevo equipo de maquinaria", description = "Registra una nueva maquinaria/activo con código y estado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Maquinaria registrada exitosamente", content = @Content(schema = @Schema(implementation = MachineryResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Código de maquinaria duplicado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<MachineryResponseDTO> createMachinery(
            @Valid @RequestBody MachineryCreateDTO requestDTO,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        MachineryResponseDTO created = machineryService.createMachinery(requestDTO, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA', 'ALMACEN', 'TECNICO')")
    @Operation(summary = "Listar maquinaria con paginación y filtros", description = "Obtiene la lista paginada de maquinaria y activos operativos.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado paginado de maquinaria obtenido exitosamente", content = @Content(schema = @Schema(implementation = PageResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<PageResponseDTO<MachineryResponseDTO>> getMachinery(
            @Parameter(description = "ID de la empresa (opcional para SUPERADMIN)") @RequestParam(required = false) UUID companyId,
            @Parameter(description = "Número de página (0..N)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Cantidad de elementos por página") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Búsqueda por código o nombre") @RequestParam(required = false) String search,
            @Parameter(description = "Filtrar por estado (DISPONIBLE, EN_USO, EN_MANTENIMIENTO, DE_BAJA)") @RequestParam(required = false) String status,
            @Parameter(description = "Criterio de ordenamiento (ej. createdAt,desc)") @RequestParam(defaultValue = "createdAt,desc") String sort,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        PageResponseDTO<MachineryResponseDTO> machinery = machineryService.getMachineryPaged(
                companyId, page, size, search, status, sort, currentUser
        );
        return ResponseEntity.ok(machinery);
    }

    @PostMapping("/search")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA', 'ALMACEN', 'TECNICO')")
    @Operation(summary = "Búsqueda avanzada de maquinaria (Body JSON)", description = "Búsqueda estructurada de maquinaria y activos.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resultados de búsqueda obtenidos exitosamente", content = @Content(schema = @Schema(implementation = PageResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<PageResponseDTO<MachineryResponseDTO>> queryMachinery(
            @RequestBody MachineryFilterDTO filter,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        PageResponseDTO<MachineryResponseDTO> result = machineryService.queryMachinery(filter, currentUser);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA', 'ALMACEN', 'TECNICO')")
    @Operation(summary = "Obtener maquinaria por ID", description = "Obtiene los detalles del activo especificado por UUID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Maquinaria encontrada exitosamente", content = @Content(schema = @Schema(implementation = MachineryResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Maquinaria no encontrada", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<MachineryResponseDTO> getMachineryById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        MachineryResponseDTO machinery = machineryService.getMachineryById(id, currentUser);
        return ResponseEntity.ok(machinery);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA')")
    @Operation(summary = "Actualizar datos de maquinaria", description = "Actualiza el nombre, código y fechas de mantenimiento de un equipo.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Maquinaria actualizada exitosamente", content = @Content(schema = @Schema(implementation = MachineryResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Maquinaria no encontrada", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Código en conflicto con otro equipo", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<MachineryResponseDTO> updateMachinery(
            @PathVariable UUID id,
            @Valid @RequestBody MachineryUpdateDTO requestDTO,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        MachineryResponseDTO updated = machineryService.updateMachinery(id, requestDTO, currentUser);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'ALMACEN', 'TECNICO')")
    @Operation(summary = "Cambiar estado operativo de la maquinaria", description = "Actualiza el estado de la maquinaria (DISPONIBLE, EN_USO, EN_MANTENIMIENTO, DE_BAJA).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado de maquinaria actualizado exitosamente", content = @Content(schema = @Schema(implementation = MachineryResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Estado inválido", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Maquinaria no encontrada", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<MachineryResponseDTO> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody MachineryStatusUpdateDTO requestDTO,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        MachineryResponseDTO updated = machineryService.updateStatus(id, requestDTO, currentUser);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA')")
    @Operation(summary = "Eliminar maquinaria (Soft Delete)", description = "Realiza la eliminación lógica del equipo de maquinaria.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Maquinaria eliminada lógicamente exitosamente"),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Maquinaria no encontrada", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<Void> deleteMachinery(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        machineryService.deleteMachinery(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
