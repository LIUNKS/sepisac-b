package com.sepisac.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Respuesta detallada de un movimiento de inventario / Kardex")
public class InventoryMovementResponseDTO {

    @Schema(description = "ID único del movimiento")
    private UUID id;

    @Schema(description = "ID de la empresa tenant")
    private UUID companyId;

    @Schema(description = "ID del ítem de inventario")
    private UUID inventoryItemId;

    @Schema(description = "Código SKU del material")
    private String inventoryItemSku;

    @Schema(description = "Nombre o descripción del material")
    private String inventoryItemName;

    @Schema(description = "ID del usuario que registró el movimiento")
    private UUID userId;

    @Schema(description = "Nombre completo del usuario que registró el movimiento")
    private String userFullName;

    @Schema(description = "Tipo de movimiento (ENTRADA, SALIDA, AJUSTE)", example = "ENTRADA")
    private String movementType;

    @Schema(description = "Cantidad modificada", example = "25")
    private Integer quantityChanged;

    @Schema(description = "Motivo o referencia del movimiento", example = "Recepción de OC OC-2026-001")
    private String reason;

    @Schema(description = "Fecha y hora del movimiento")
    private OffsetDateTime createdAt;

    public InventoryMovementResponseDTO() {
    }

    public InventoryMovementResponseDTO(UUID id, UUID companyId, UUID inventoryItemId, String inventoryItemSku,
                                        String inventoryItemName, UUID userId, String userFullName,
                                        String movementType, Integer quantityChanged, String reason,
                                        OffsetDateTime createdAt) {
        this.id = id;
        this.companyId = companyId;
        this.inventoryItemId = inventoryItemId;
        this.inventoryItemSku = inventoryItemSku;
        this.inventoryItemName = inventoryItemName;
        this.userId = userId;
        this.userFullName = userFullName;
        this.movementType = movementType;
        this.quantityChanged = quantityChanged;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }

    public UUID getInventoryItemId() {
        return inventoryItemId;
    }

    public void setInventoryItemId(UUID inventoryItemId) {
        this.inventoryItemId = inventoryItemId;
    }

    public String getInventoryItemSku() {
        return inventoryItemSku;
    }

    public void setInventoryItemSku(String inventoryItemSku) {
        this.inventoryItemSku = inventoryItemSku;
    }

    public String getInventoryItemName() {
        return inventoryItemName;
    }

    public void setInventoryItemName(String inventoryItemName) {
        this.inventoryItemName = inventoryItemName;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public String getMovementType() {
        return movementType;
    }

    public void setMovementType(String movementType) {
        this.movementType = movementType;
    }

    public Integer getQuantityChanged() {
        return quantityChanged;
    }

    public void setQuantityChanged(Integer quantityChanged) {
        this.quantityChanged = quantityChanged;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
