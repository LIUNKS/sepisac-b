package com.sepisac.backend.controller;

import com.sepisac.backend.dto.ErrorResponseDTO;
import com.sepisac.backend.dto.PageResponseDTO;
import com.sepisac.backend.dto.SupplierCreateDTO;
import com.sepisac.backend.dto.SupplierFilterDTO;
import com.sepisac.backend.dto.SupplierResponseDTO;
import com.sepisac.backend.dto.SupplierUpdateDTO;
import com.sepisac.backend.security.UserPrincipal;
import com.sepisac.backend.service.SupplierService;
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
@RequestMapping({"/api/v1/suppliers", "/api/suppliers"})
@Tag(name = "Suppliers", description = "Endpoints para la gestión del Catálogo de Proveedores de la Empresa")
@SecurityRequirement(name = "bearerAuth")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'ALMACEN')")
    @Operation(summary = "Registrar nuevo proveedor", description = "Registra un nuevo proveedor en la empresa validando RUC.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Proveedor registrado exitosamente", content = @Content(schema = @Schema(implementation = SupplierResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o formato RUC incorrecto", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "RUC ya registrado en la empresa", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<SupplierResponseDTO> createSupplier(
            @Valid @RequestBody SupplierCreateDTO requestDTO,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        SupplierResponseDTO created = supplierService.createSupplier(requestDTO, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA', 'ALMACEN')")
    @Operation(summary = "Listar proveedores con paginación y filtros", description = "Obtiene la lista paginada de proveedores activos de la empresa.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado paginado de proveedores obtenido exitosamente", content = @Content(schema = @Schema(implementation = PageResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<PageResponseDTO<SupplierResponseDTO>> getSuppliers(
            @Parameter(description = "ID de la empresa (opcional para SUPERADMIN)") @RequestParam(required = false) UUID companyId,
            @Parameter(description = "Número de página (0..N)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Cantidad de elementos por página") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Búsqueda por RUC o Razón Social") @RequestParam(required = false) String search,
            @Parameter(description = "Criterio de ordenamiento (ej. createdAt,desc)") @RequestParam(defaultValue = "createdAt,desc") String sort,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        PageResponseDTO<SupplierResponseDTO> suppliers = supplierService.getSuppliersPaged(
                companyId, page, size, search, sort, currentUser
        );
        return ResponseEntity.ok(suppliers);
    }

    @PostMapping("/search")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA', 'ALMACEN')")
    @Operation(summary = "Búsqueda avanzada de proveedores (Body JSON)", description = "Búsqueda estructurada de proveedores.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resultados de búsqueda obtenidos exitosamente", content = @Content(schema = @Schema(implementation = PageResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<PageResponseDTO<SupplierResponseDTO>> querySuppliers(
            @RequestBody SupplierFilterDTO filter,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        PageResponseDTO<SupplierResponseDTO> result = supplierService.querySuppliers(filter, currentUser);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/check-ruc")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA', 'ALMACEN')")
    @Operation(summary = "Verificar existencia de RUC", description = "Verifica si un RUC ya se encuentra registrado para la empresa.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Verificación realizada exitosamente"),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<java.util.Map<String, Boolean>> checkRuc(
            @RequestParam String ruc,
            @RequestParam(required = false) UUID companyId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        boolean exists = supplierService.checkRucExists(ruc, companyId, currentUser);
        return ResponseEntity.ok(java.util.Map.of("exists", exists));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA', 'ALMACEN')")
    @Operation(summary = "Obtener proveedor por ID", description = "Obtiene los detalles del proveedor especificado por UUID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Proveedor encontrado exitosamente", content = @Content(schema = @Schema(implementation = SupplierResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Proveedor no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<SupplierResponseDTO> getSupplierById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        SupplierResponseDTO supplier = supplierService.getSupplierById(id, currentUser);
        return ResponseEntity.ok(supplier);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'ALMACEN')")
    @Operation(summary = "Actualizar proveedor", description = "Actualiza los datos de un proveedor existente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Proveedor actualizado exitosamente", content = @Content(schema = @Schema(implementation = SupplierResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Proveedor no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "RUC en conflicto con otro proveedor de la empresa", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<SupplierResponseDTO> updateSupplier(
            @PathVariable UUID id,
            @Valid @RequestBody SupplierUpdateDTO requestDTO,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        SupplierResponseDTO updated = supplierService.updateSupplier(id, requestDTO, currentUser);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'ALMACEN')")
    @Operation(summary = "Eliminar proveedor (Soft Delete)", description = "Realiza la eliminación lógica del proveedor.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Proveedor eliminado lógicamente exitosamente"),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Proveedor no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<Void> deleteSupplier(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        supplierService.deleteSupplier(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
