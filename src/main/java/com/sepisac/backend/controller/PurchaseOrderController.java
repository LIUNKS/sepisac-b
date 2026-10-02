package com.sepisac.backend.controller;

import com.sepisac.backend.dto.*;
import com.sepisac.backend.exception.BusinessRuleException;
import com.sepisac.backend.security.UserPrincipal;
import com.sepisac.backend.service.PurchaseOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping({"/api/v1/purchase-orders", "/api/purchase-orders"})
@Tag(name = "Purchase Orders", description = "Endpoints para la gestión, emisión y cancelación de Órdenes de Compra")
@SecurityRequirement(name = "bearerAuth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }


    @GetMapping("/auto-generate/preview")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'ALMACEN')")
    @Operation(summary = "Previsualizar generación automática de órdenes de compra", description = "Muestra las órdenes que se generarían sin guardarlas.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Previsualización generada",
                    content = @Content(schema = @Schema(implementation = PurchaseOrderResponseDTO.class)))
    })
    public ResponseEntity<List<PurchaseOrderResponseDTO>> previewAutoGenerateOrders(
            @Parameter(description = "ID de la empresa (opcional para SUPERADMIN)") @RequestParam(required = false) UUID companyId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        UUID targetCompanyId = resolveCompanyId(companyId, currentUser);
        List<PurchaseOrderResponseDTO> previewList = purchaseOrderService.previewAutoGenerateOrders(targetCompanyId);
        return ResponseEntity.ok(previewList);
    }

    @PostMapping("/auto-generate")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'ALMACEN')")
    @Operation(summary = "Generación automática de órdenes de compra", description = "Evalúa ítems críticos y crea órdenes automáticas agrupadas por proveedor.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Órdenes generadas o evaluadas exitosamente",
                    content = @Content(schema = @Schema(implementation = AutoGenerateOrdersResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<AutoGenerateOrdersResponseDTO> autoGenerateOrders(
            @Parameter(description = "ID de la empresa (opcional para SUPERADMIN)") @RequestParam(required = false) UUID companyId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        UUID targetCompanyId = resolveCompanyId(companyId, currentUser);
        UUID userId = currentUser != null ? currentUser.getId() : null;
        AutoGenerateOrdersResponseDTO response = purchaseOrderService.autoGenerateOrders(targetCompanyId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'ALMACEN')")
    @Operation(summary = "Crear orden de compra manual", description = "Registra una orden de compra manual validando proveedor e ítems.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Orden de compra creada exitosamente",
                    content = @Content(schema = @Schema(implementation = PurchaseOrderResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Proveedor o ítem no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<PurchaseOrderResponseDTO> createManualOrder(
            @Valid @RequestBody CreatePurchaseOrderRequestDTO request,
            @Parameter(description = "ID de la empresa (opcional para SUPERADMIN)") @RequestParam(required = false) UUID companyId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        UUID targetCompanyId = resolveCompanyId(companyId, currentUser);
        UUID userId = currentUser != null ? currentUser.getId() : null;
        PurchaseOrderResponseDTO created = purchaseOrderService.createManualOrder(targetCompanyId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA', 'ALMACEN')")
    @Operation(summary = "Listar órdenes de compra", description = "Obtiene el listado paginado de órdenes de compra con filtro de estado opcional.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado de órdenes obtenido exitosamente",
                    content = @Content(schema = @Schema(implementation = PageResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<PageResponseDTO<PurchaseOrderResponseDTO>> getOrders(
            @Parameter(description = "Filtrar por estado (ej. PENDIENTE, RECIBIDA, CANCELADA)") @RequestParam(required = false) String status,
            @Parameter(description = "Número de página (0..N)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Cantidad de elementos por página") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "ID de la empresa (opcional para SUPERADMIN)") @RequestParam(required = false) UUID companyId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        UUID targetCompanyId = resolveCompanyId(companyId, currentUser);
        Pageable pageable = PageRequest.of(page, size);
        PageResponseDTO<PurchaseOrderResponseDTO> orders = purchaseOrderService.getOrdersByStatus(targetCompanyId, status, pageable);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA', 'ALMACEN')")
    @Operation(summary = "Obtener orden de compra por ID", description = "Obtiene los detalles completos de una orden de compra.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orden encontrada exitosamente",
                    content = @Content(schema = @Schema(implementation = PurchaseOrderResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Orden de compra no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<PurchaseOrderResponseDTO> getOrderById(
            @PathVariable UUID id,
            @Parameter(description = "ID de la empresa (opcional para SUPERADMIN)") @RequestParam(required = false) UUID companyId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        UUID targetCompanyId = resolveCompanyId(companyId, currentUser);
        PurchaseOrderResponseDTO order = purchaseOrderService.getOrderById(targetCompanyId, id);
        return ResponseEntity.ok(order);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'ALMACEN')")
    @Operation(summary = "Actualizar estado de orden de compra (Cancelar)", description = "Permite cancelar una orden en estado PENDIENTE.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orden cancelada exitosamente",
                    content = @Content(schema = @Schema(implementation = PurchaseOrderResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Estado no válido para cancelación",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Orden de compra no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Estado actual no permite cancelación (PO_INVALID_STATE)",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<PurchaseOrderResponseDTO> updateOrderStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePurchaseOrderStatusRequestDTO request,
            @Parameter(description = "ID de la empresa (opcional para SUPERADMIN)") @RequestParam(required = false) UUID companyId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        if (request.getStatus() == null || !request.getStatus().trim().equalsIgnoreCase("CANCELADA")) {
            throw new BusinessRuleException("Solo se permite cambiar el estado a CANCELADA");
        }
        UUID targetCompanyId = resolveCompanyId(companyId, currentUser);
        UUID userId = currentUser != null ? currentUser.getId() : null;
        PurchaseOrderResponseDTO updated = purchaseOrderService.cancelOrder(targetCompanyId, userId, id);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'ALMACEN')")
    @Operation(summary = "Cancelar orden de compra directa", description = "Atajo para cancelar una orden de compra en estado PENDIENTE.")
    public ResponseEntity<PurchaseOrderResponseDTO> cancelOrder(
            @PathVariable UUID id,
            @Parameter(description = "ID de la empresa (opcional para SUPERADMIN)") @RequestParam(required = false) UUID companyId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        UUID targetCompanyId = resolveCompanyId(companyId, currentUser);
        UUID userId = currentUser != null ? currentUser.getId() : null;
        PurchaseOrderResponseDTO updated = purchaseOrderService.cancelOrder(targetCompanyId, userId, id);
        return ResponseEntity.ok(updated);
    }

    private UUID resolveCompanyId(UUID companyId, UserPrincipal currentUser) {
        if (currentUser != null && "ROLE_SUPERADMIN".equals(currentUser.getRole())) {
            return companyId != null ? companyId : currentUser.getCompanyId();
        }
        return currentUser != null ? currentUser.getCompanyId() : companyId;
    }
}
