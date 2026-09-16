package com.sepisac.backend.controller;

import com.sepisac.backend.dto.*;
import com.sepisac.backend.security.UserPrincipal;
import com.sepisac.backend.service.QuotationService;
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
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/quotations")
@Tag(name = "Commercial & Quotations", description = "Endpoints para la gestión de Cotizaciones Comerciales, Estructura de Costos, Requerimientos de Mano de Obra y Estados")
@SecurityRequirement(name = "bearerAuth")
public class QuotationController {

    private final QuotationService quotationService;

    public QuotationController(QuotationService quotationService) {
        this.quotationService = quotationService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA')")
    @Operation(summary = "Registrar nueva cotización", description = "Crea una nueva cotización comercial con o sin ítems y mano de obra iniciales.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cotización registrada exitosamente", content = @Content(schema = @Schema(implementation = QuotationFullDetailResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Número de cotización duplicado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<QuotationFullDetailResponseDTO> createQuotation(
            @Valid @RequestBody QuotationCreateDTO requestDTO,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        QuotationFullDetailResponseDTO created = quotationService.createQuotation(requestDTO, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA', 'ALMACEN', 'TECNICO')")
    @Operation(summary = "Listar cotizaciones con paginación y filtros", description = "Obtiene la lista paginada de cotizaciones con filtros por empresa, estado, tipo de servicio y búsqueda de texto.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado paginado de cotizaciones obtenido exitosamente", content = @Content(schema = @Schema(implementation = PageResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<PageResponseDTO<QuotationResponseDTO>> getQuotations(
            @Parameter(description = "ID de la empresa (opcional para SUPERADMIN)") @RequestParam(required = false) UUID companyId,
            @Parameter(description = "Número de página (0..N)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Cantidad de elementos por página") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Búsqueda por número de cotización o nombre de cliente") @RequestParam(required = false) String search,
            @Parameter(description = "Filtrar por estado (BORRADOR, ENVIADA, APROBADA, RECHAZADA)") @RequestParam(required = false) String status,
            @Parameter(description = "Filtrar por tipo de servicio") @RequestParam(required = false) String serviceType,
            @Parameter(description = "Criterio de ordenamiento (ej. createdAt,desc)") @RequestParam(defaultValue = "createdAt,desc") String sort,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        QuotationFilterDTO filter = new QuotationFilterDTO();
        filter.setCompanyId(companyId);
        filter.setPage(page);
        filter.setSize(size);
        filter.setSearch(search);
        filter.setStatus(status);
        filter.setServiceType(serviceType);
        filter.setSort(sort);

        PageResponseDTO<QuotationResponseDTO> response = quotationService.queryQuotations(filter, currentUser);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/search")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA', 'ALMACEN', 'TECNICO')")
    @Operation(summary = "Búsqueda avanzada de cotizaciones (Body JSON)", description = "Búsqueda estructurada de cotizaciones vía cuerpo JSON.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resultados de búsqueda obtenidos exitosamente", content = @Content(schema = @Schema(implementation = PageResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<PageResponseDTO<QuotationResponseDTO>> queryQuotations(
            @RequestBody QuotationFilterDTO filter,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        PageResponseDTO<QuotationResponseDTO> result = quotationService.queryQuotations(filter, currentUser);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA', 'ALMACEN', 'TECNICO')")
    @Operation(summary = "Obtener cotización completa por ID", description = "Obtiene los detalles completos de la cotización, incluyendo ítems y requerimientos de mano de obra.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cotización encontrada exitosamente", content = @Content(schema = @Schema(implementation = QuotationFullDetailResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Cotización no encontrada", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<QuotationFullDetailResponseDTO> getQuotationById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        QuotationFullDetailResponseDTO quotation = quotationService.getQuotationById(id, currentUser);
        return ResponseEntity.ok(quotation);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA')")
    @Operation(summary = "Actualizar cabecera de cotización", description = "Actualiza los datos comerciales de la cabecera (solo en estado BORRADOR).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cotización actualizada exitosamente", content = @Content(schema = @Schema(implementation = QuotationResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Cotización no encontrada", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Operación no permitida si no está en BORRADOR", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<QuotationResponseDTO> updateQuotation(
            @PathVariable UUID id,
            @Valid @RequestBody QuotationUpdateDTO requestDTO,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        QuotationResponseDTO updated = quotationService.updateQuotation(id, requestDTO, currentUser);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA')")
    @Operation(summary = "Actualizar estado de la cotización", description = "Transiciona el estado de la cotización siguiendo la máquina de estados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado de cotización actualizado exitosamente", content = @Content(schema = @Schema(implementation = QuotationResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Estado inválido", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Cotización no encontrada", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Transición de estado inválida", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<QuotationResponseDTO> updateQuotationStatus(
            @PathVariable UUID id,
            @Valid @RequestBody QuotationStatusUpdateDTO requestDTO,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        QuotationResponseDTO updated = quotationService.updateQuotationStatus(id, requestDTO, currentUser);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA')")
    @Operation(summary = "Eliminar cotización (Soft Delete)", description = "Elimina lógicamente la cotización especificada (no permitido si está en estado APROBADA).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Cotización eliminada exitosamente"),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Cotización no encontrada", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar una cotización APROBADA", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<Void> deleteQuotation(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        quotationService.deleteQuotation(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    // Detail Items Endpoints
    @PostMapping("/{id}/details")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA')")
    @Operation(summary = "Agregar ítem a la cotización", description = "Agrega un nuevo ítem de material/equipo/repuesto y recalcula automáticamente los totales de la cotización.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Ítem agregado exitosamente", content = @Content(schema = @Schema(implementation = QuotationDetailResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Cotización no encontrada", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "No permitido si la cotización no está en BORRADOR", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<QuotationDetailResponseDTO> addDetail(
            @PathVariable UUID id,
            @Valid @RequestBody QuotationDetailCreateDTO requestDTO,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        QuotationDetailResponseDTO created = quotationService.addDetail(id, requestDTO, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}/details/{detailId}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA')")
    @Operation(summary = "Actualizar ítem de la cotización", description = "Actualiza descripción, cantidad o precio de un ítem y recalcula los totales.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ítem actualizado exitosamente", content = @Content(schema = @Schema(implementation = QuotationDetailResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Ítem o cotización no encontrados", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "No permitido si la cotización no está en BORRADOR", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<QuotationDetailResponseDTO> updateDetail(
            @PathVariable UUID id,
            @PathVariable UUID detailId,
            @Valid @RequestBody QuotationDetailUpdateDTO requestDTO,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        QuotationDetailResponseDTO updated = quotationService.updateDetail(id, detailId, requestDTO, currentUser);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}/details/{detailId}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA')")
    @Operation(summary = "Eliminar ítem de la cotización", description = "Elimina un ítem y recalcula automáticamente los totales de la cotización.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Ítem eliminado exitosamente"),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Ítem o cotización no encontrados", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "No permitido si la cotización no está en BORRADOR", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<Void> deleteDetail(
            @PathVariable UUID id,
            @PathVariable UUID detailId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        quotationService.deleteDetail(id, detailId, currentUser);
        return ResponseEntity.noContent().build();
    }

    // Labor Requirements Endpoints
    @PostMapping("/{id}/labor")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA')")
    @Operation(summary = "Agregar requerimiento de mano de obra", description = "Agrega una línea de mano de obra y recalcula automáticamente los totales de la cotización.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Requerimiento de mano de obra agregado exitosamente", content = @Content(schema = @Schema(implementation = QuotationLaborResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Cotización no encontrada", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "No permitido si la cotización no está en BORRADOR", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<QuotationLaborResponseDTO> addLaborRequirement(
            @PathVariable UUID id,
            @Valid @RequestBody QuotationLaborCreateDTO requestDTO,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        QuotationLaborResponseDTO created = quotationService.addLaborRequirement(id, requestDTO, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}/labor/{laborId}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA')")
    @Operation(summary = "Actualizar requerimiento de mano de obra", description = "Actualiza horas, cantidad o costo por hora de mano de obra y recalcula los totales.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Requerimiento de mano de obra actualizado exitosamente", content = @Content(schema = @Schema(implementation = QuotationLaborResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Requerimiento o cotización no encontrados", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "No permitido si la cotización no está en BORRADOR", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<QuotationLaborResponseDTO> updateLaborRequirement(
            @PathVariable UUID id,
            @PathVariable UUID laborId,
            @Valid @RequestBody QuotationLaborUpdateDTO requestDTO,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        QuotationLaborResponseDTO updated = quotationService.updateLaborRequirement(id, laborId, requestDTO, currentUser);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}/labor/{laborId}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA')")
    @Operation(summary = "Eliminar requerimiento de mano de obra", description = "Elimina una línea de mano de obra y recalcula automáticamente los totales de la cotización.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Requerimiento de mano de obra eliminado exitosamente"),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Requerimiento o cotización no encontrados", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "No permitido si la cotización no está en BORRADOR", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<Void> deleteLaborRequirement(
            @PathVariable UUID id,
            @PathVariable UUID laborId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        quotationService.deleteLaborRequirement(id, laborId, currentUser);
        return ResponseEntity.noContent().build();
    }
}
