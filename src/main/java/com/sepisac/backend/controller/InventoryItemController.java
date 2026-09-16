package com.sepisac.backend.controller;

import com.sepisac.backend.dto.ErrorResponseDTO;
import com.sepisac.backend.dto.InventoryItemCreateDTO;
import com.sepisac.backend.dto.InventoryItemFilterDTO;
import com.sepisac.backend.dto.InventoryItemResponseDTO;
import com.sepisac.backend.dto.InventoryItemUpdateDTO;
import com.sepisac.backend.dto.PageResponseDTO;
import com.sepisac.backend.security.UserPrincipal;
import com.sepisac.backend.service.InventoryItemService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/inventory/items")
@Tag(name = "InventoryItems", description = "Endpoints para la gestión del Catálogo Maestro de Ítems de Inventario y Almacén")
@SecurityRequirement(name = "bearerAuth")
public class InventoryItemController {

    private final InventoryItemService inventoryItemService;

    public InventoryItemController(InventoryItemService inventoryItemService) {
        this.inventoryItemService = inventoryItemService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'ALMACEN')")
    @Operation(summary = "Crear nuevo ítem de inventario", description = "Registra un nuevo producto/material en el catálogo maestro.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Ítem creado exitosamente", content = @Content(schema = @Schema(implementation = InventoryItemResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o costo negativo", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "SKU ya registrado en la empresa", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<InventoryItemResponseDTO> createItem(
            @Valid @RequestBody InventoryItemCreateDTO requestDTO,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        InventoryItemResponseDTO created = inventoryItemService.createItem(requestDTO, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA', 'ALMACEN', 'TECNICO')")
    @Operation(summary = "Listar ítems de inventario con paginación y filtros", description = "Obtiene la lista paginada de productos de inventario.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado paginado de inventario obtenido exitosamente", content = @Content(schema = @Schema(implementation = PageResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<PageResponseDTO<InventoryItemResponseDTO>> getItems(
            @Parameter(description = "ID de la empresa (opcional para SUPERADMIN)") @RequestParam(required = false) UUID companyId,
            @Parameter(description = "Número de página (0..N)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Cantidad de elementos por página") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Búsqueda por SKU o Nombre") @RequestParam(required = false) String search,
            @Parameter(description = "Filtrar por alerta de bajo stock (true/false)") @RequestParam(required = false) Boolean lowStock,
            @Parameter(description = "Criterio de ordenamiento (ej. createdAt,desc)") @RequestParam(defaultValue = "createdAt,desc") String sort,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        PageResponseDTO<InventoryItemResponseDTO> items = inventoryItemService.getItemsPaged(
                companyId, page, size, search, lowStock, sort, currentUser
        );
        return ResponseEntity.ok(items);
    }

    @PostMapping("/search")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA', 'ALMACEN', 'TECNICO')")
    @Operation(summary = "Búsqueda avanzada de inventario (Body JSON)", description = "Búsqueda estructurada de ítems de inventario.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resultados de búsqueda obtenidos exitosamente", content = @Content(schema = @Schema(implementation = PageResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<PageResponseDTO<InventoryItemResponseDTO>> queryItems(
            @RequestBody InventoryItemFilterDTO filter,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        PageResponseDTO<InventoryItemResponseDTO> result = inventoryItemService.queryItems(filter, currentUser);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA', 'ALMACEN', 'TECNICO')")
    @Operation(summary = "Obtener ítem de inventario por ID", description = "Obtiene los detalles del producto especificado por UUID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ítem encontrado exitosamente", content = @Content(schema = @Schema(implementation = InventoryItemResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Ítem no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<InventoryItemResponseDTO> getItemById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        InventoryItemResponseDTO item = inventoryItemService.getItemById(id, currentUser);
        return ResponseEntity.ok(item);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'ALMACEN')")
    @Operation(summary = "Actualizar ítem de inventario", description = "Actualiza la descripción, precios y alertas de stock de un ítem.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ítem actualizado exitosamente", content = @Content(schema = @Schema(implementation = InventoryItemResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Ítem no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "SKU en conflicto con otro ítem", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<InventoryItemResponseDTO> updateItem(
            @PathVariable UUID id,
            @Valid @RequestBody InventoryItemUpdateDTO requestDTO,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        InventoryItemResponseDTO updated = inventoryItemService.updateItem(id, requestDTO, currentUser);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'ALMACEN')")
    @Operation(summary = "Eliminar ítem de inventario (Soft Delete)", description = "Realiza la eliminación lógica del ítem de inventario.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Ítem eliminado lógicamente exitosamente"),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Ítem no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<Void> deleteItem(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        inventoryItemService.deleteItem(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
