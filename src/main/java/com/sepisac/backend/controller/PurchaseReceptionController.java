package com.sepisac.backend.controller;

import com.sepisac.backend.dto.ErrorResponseDTO;
import com.sepisac.backend.dto.PurchaseReceptionResponseDTO;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/purchase-orders/{id}/receptions", "/api/purchase-orders/{id}/receptions"})
@Tag(name = "Purchase Receptions", description = "Endpoints para la recepción de Órdenes de Compra y actualización de inventario")
@SecurityRequirement(name = "bearerAuth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class PurchaseReceptionController {

    private final PurchaseOrderService purchaseOrderService;

    public PurchaseReceptionController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'ALMACEN')")
    @Operation(summary = "Registrar recepción de orden de compra",
            description = "Recibe los materiales de una orden de compra en estado PENDIENTE, recalculando costo promedio ponderado y registrando movimientos.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recepción procesada exitosamente",
                    content = @Content(schema = @Schema(implementation = PurchaseReceptionResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Orden de compra no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Orden no se encuentra en estado PENDIENTE (PO_INVALID_STATE)",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "422", description = "Ítem de la orden no disponible o eliminado (PO_ITEM_UNAVAILABLE)",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<PurchaseReceptionResponseDTO> receiveOrder(
            @PathVariable UUID id,
            @Parameter(description = "ID de la empresa (opcional para SUPERADMIN)") @RequestParam(required = false) UUID companyId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        UUID targetCompanyId = resolveCompanyId(companyId, currentUser);
        UUID userId = currentUser != null ? currentUser.getId() : null;
        PurchaseReceptionResponseDTO response = purchaseOrderService.receiveOrder(targetCompanyId, userId, id);
        return ResponseEntity.ok(response);
    }

    private UUID resolveCompanyId(UUID companyId, UserPrincipal currentUser) {
        if (currentUser != null && "ROLE_SUPERADMIN".equals(currentUser.getRole())) {
            return companyId != null ? companyId : currentUser.getCompanyId();
        }
        return currentUser != null ? currentUser.getCompanyId() : companyId;
    }
}
