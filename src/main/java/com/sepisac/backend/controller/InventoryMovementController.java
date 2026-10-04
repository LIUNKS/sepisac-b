package com.sepisac.backend.controller;

import com.sepisac.backend.dto.ErrorResponseDTO;
import com.sepisac.backend.dto.InventoryMovementFilterDTO;
import com.sepisac.backend.dto.InventoryMovementResponseDTO;
import com.sepisac.backend.dto.PageResponseDTO;
import com.sepisac.backend.security.UserPrincipal;
import com.sepisac.backend.service.InventoryMovementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/inventory/movements", "/api/inventory/movements"})
@Tag(name = "Inventory Movements", description = "Endpoints para la consulta del Kardex e historial de movimientos de inventario")
@SecurityRequirement(name = "bearerAuth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class InventoryMovementController {

    private final InventoryMovementService movementService;

    public InventoryMovementController(InventoryMovementService movementService) {
        this.movementService = movementService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'ALMACEN', 'GERENCIA')")
    @Operation(summary = "Consultar Kardex / Movimientos de inventario con filtros y paginación",
            description = "Obtiene la lista paginada de movimientos de inventario (entradas, salidas) filtrando por empresa, ítem, tipo y fechas.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado de movimientos obtenido exitosamente",
                    content = @Content(schema = @Schema(implementation = PageResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado a otra empresa",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<PageResponseDTO<InventoryMovementResponseDTO>> getMovements(
            @Parameter(description = "ID de la empresa (opcional para SUPERADMIN)") @RequestParam(required = false) UUID companyId,
            @Parameter(description = "Filtrar por ID del material/ítem") @RequestParam(required = false) UUID inventoryItemId,
            @Parameter(description = "Tipo de movimiento (ENTRADA, SALIDA)", example = "ENTRADA") @RequestParam(required = false) String movementType,
            @Parameter(description = "Fecha inicial (ISO-8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @Parameter(description = "Fecha final (ISO-8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate,
            @Parameter(description = "Número de página (0..N)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Cantidad de elementos por página") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Criterio de ordenamiento (ej. createdAt,desc)") @RequestParam(defaultValue = "createdAt,desc") String sort,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        PageResponseDTO<InventoryMovementResponseDTO> response = movementService.getMovementsPaged(
                companyId, inventoryItemId, movementType, startDate, endDate, page, size, sort, currentUser
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/search")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'ALMACEN', 'GERENCIA')")
    @Operation(summary = "Búsqueda avanzada estructurada de movimientos (Body JSON)",
            description = "Permite enviar criterios de búsqueda en un cuerpo JSON.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resultados obtenidos exitosamente",
                    content = @Content(schema = @Schema(implementation = PageResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<PageResponseDTO<InventoryMovementResponseDTO>> queryMovements(
            @RequestBody InventoryMovementFilterDTO filter,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        PageResponseDTO<InventoryMovementResponseDTO> response = movementService.queryMovements(filter, currentUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/items/{itemId}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'ALMACEN', 'GERENCIA')")
    @Operation(summary = "Consultar historial de movimientos de un material específico",
            description = "Obtiene los movimientos históricos asociados a un ítem de inventario determinado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Historial del material obtenido exitosamente",
                    content = @Content(schema = @Schema(implementation = PageResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Material no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<PageResponseDTO<InventoryMovementResponseDTO>> getMovementsByItemId(
            @Parameter(description = "ID del material") @PathVariable UUID itemId,
            @Parameter(description = "Número de página (0..N)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Cantidad de elementos por página") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Criterio de ordenamiento (ej. createdAt,desc)") @RequestParam(defaultValue = "createdAt,desc") String sort,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        PageResponseDTO<InventoryMovementResponseDTO> response = movementService.getMovementsByItemIdPaged(
                itemId, page, size, sort, currentUser
        );
        return ResponseEntity.ok(response);
    }
}
