package com.sepisac.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Filtros para la consulta estructurada de movimientos de inventario")
public class InventoryMovementFilterDTO {

    @Schema(description = "ID de la empresa (solo para SUPERADMIN)")
    private UUID companyId;

    @Schema(description = "Filtrar por ID del ítem de inventario")
    private UUID inventoryItemId;

    @Schema(description = "Filtrar por tipo de movimiento (ENTRADA, SALIDA)", example = "ENTRADA")
    private String movementType;

    @Schema(description = "Fecha inicial de creación (ISO-8601)")
    private OffsetDateTime startDate;

    @Schema(description = "Fecha final de creación (ISO-8601)")
    private OffsetDateTime endDate;

    @Schema(description = "Número de página (inicia en 0)", example = "0")
    private int page = 0;

    @Schema(description = "Cantidad de elementos por página", example = "10")
    private int size = 10;

    @Schema(description = "Criterio de ordenamiento (ej. createdAt,desc)", example = "createdAt,desc")
    private String sort = "createdAt,desc";

    public InventoryMovementFilterDTO() {
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

    public String getMovementType() {
        return movementType;
    }

    public void setMovementType(String movementType) {
        this.movementType = movementType;
    }

    public OffsetDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(OffsetDateTime startDate) {
        this.startDate = startDate;
    }

    public OffsetDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(OffsetDateTime endDate) {
        this.endDate = endDate;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }
}
