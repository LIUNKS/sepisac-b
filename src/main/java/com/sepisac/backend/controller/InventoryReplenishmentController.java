package com.sepisac.backend.controller;

import com.sepisac.backend.dto.ErrorResponseDTO;
import com.sepisac.backend.dto.InventoryReplenishmentSettingsRequestDTO;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/inventory-items/{id}/replenishment-settings", "/api/inventory/items/{id}/replenishment-settings"})
@Tag(name = "Inventory Replenishment", description = "Endpoints para la parametrización de reposición automática y stock mínimo de inventario")
@SecurityRequirement(name = "bearerAuth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class InventoryReplenishmentController {

    private final PurchaseOrderService purchaseOrderService;

    public InventoryReplenishmentController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'ALMACEN')")
    @Operation(summary = "Actualizar configuración de reposición de ítem",
            description = "Configura el proveedor asignado, stock mínimo de alerta y cantidad sugerida de reorden para un ítem.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Configuración actualizada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos o valores negativos",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Ítem o proveedor no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<Map<String, String>> updateReplenishmentSettings(
            @PathVariable UUID id,
            @Valid @RequestBody InventoryReplenishmentSettingsRequestDTO request,
            @Parameter(description = "ID de la empresa (opcional para SUPERADMIN)") @RequestParam(required = false) UUID companyId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        UUID targetCompanyId = resolveCompanyId(companyId, currentUser);
        UUID userId = currentUser != null ? currentUser.getId() : null;
        purchaseOrderService.updateReplenishmentSettings(targetCompanyId, userId, id, request);
        return ResponseEntity.ok(Map.of("message", "Configuración de reposición actualizada exitosamente"));
    }

    private UUID resolveCompanyId(UUID companyId, UserPrincipal currentUser) {
        if (currentUser != null && "ROLE_SUPERADMIN".equals(currentUser.getRole())) {
            return companyId != null ? companyId : currentUser.getCompanyId();
        }
        return currentUser != null ? currentUser.getCompanyId() : companyId;
    }
}
