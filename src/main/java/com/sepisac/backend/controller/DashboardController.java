package com.sepisac.backend.controller;

import com.sepisac.backend.dto.*;
import com.sepisac.backend.security.UserPrincipal;
import com.sepisac.backend.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping({"/api/dashboard", "/api/v1/dashboard"})
@Tag(name = "Dashboard", description = "Endpoints para KPIs y Dashboards Gerenciales")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;
    private final com.sepisac.backend.service.ProjectSeedService projectSeedService;

    public DashboardController(DashboardService dashboardService, com.sepisac.backend.service.ProjectSeedService projectSeedService) {
        this.dashboardService = dashboardService;
        this.projectSeedService = projectSeedService;
    }

    @GetMapping("/kpis/commercial-cycle")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA')")
    @Operation(summary = "KPI de Ciclo Comercial", description = "Calcula los tiempos promedio en días por cada etapa del ciclo comercial y el total.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "KPI calculado exitosamente", content = @Content(schema = @Schema(implementation = CommercialCycleKpiResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Parámetros de fecha inválidos", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<CommercialCycleKpiResponseDTO> getCommercialCycleKpi(
            @Parameter(description = "Fecha inicial (YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Fecha final (YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "ID de la empresa (opcional para SUPERADMIN)") @RequestParam(required = false) UUID companyId,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        UUID targetCompanyId = resolveCompanyId(companyId, currentUser);
        LocalDate effectiveTo = to != null ? to : LocalDate.now();
        LocalDate effectiveFrom = from != null ? from : effectiveTo.minusMonths(6);

        CommercialCycleKpiResponseDTO response = dashboardService.getCommercialCycleKpi(targetCompanyId, effectiveFrom, effectiveTo);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/revenue")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA')")
    @Operation(summary = "KPI de Ingresos / Facturación", description = "Obtiene las series de ingresos agrupadas por día, mes o año separadas por moneda.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ingresos calculados exitosamente", content = @Content(schema = @Schema(implementation = RevenueKpiResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<RevenueKpiResponseDTO> getRevenueKpi(
            @Parameter(description = "Fecha inicial (YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Fecha final (YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "Criterio de agrupación: day, month o year") @RequestParam(defaultValue = "month") String groupBy,
            @Parameter(description = "ID de la empresa (opcional para SUPERADMIN)") @RequestParam(required = false) UUID companyId,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        UUID targetCompanyId = resolveCompanyId(companyId, currentUser);
        LocalDate effectiveTo = to != null ? to : LocalDate.now();
        LocalDate effectiveFrom = from != null ? from : effectiveTo.minusMonths(6);

        RevenueKpiResponseDTO response = dashboardService.getRevenueKpi(targetCompanyId, effectiveFrom, effectiveTo, groupBy);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/receivables")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA')")
    @Operation(summary = "KPI de Cuentas por Cobrar", description = "Calcula los saldos pendientes por cobrar agrupados por estado efectivo y moneda, con bloque de facturas pagadas.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cuentas por cobrar calculadas exitosamente", content = @Content(schema = @Schema(implementation = ReceivablesKpiResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<ReceivablesKpiResponseDTO> getReceivablesKpi(
            @Parameter(description = "ID de la empresa (opcional para SUPERADMIN)") @RequestParam(required = false) UUID companyId,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        UUID targetCompanyId = resolveCompanyId(companyId, currentUser);
        ReceivablesKpiResponseDTO response = dashboardService.getReceivablesKpi(targetCompanyId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/inventory-alerts")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA', 'ALMACEN')")
    @Operation(summary = "KPI de Alertas de Inventario", description = "Lista paginada de artículos en stock crítico y sus órdenes de compra pendientes abiertas.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alertas de inventario obtenidas exitosamente", content = @Content(schema = @Schema(implementation = PageResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<PageResponseDTO<InventoryAlertResponseDTO>> getInventoryAlerts(
            @Parameter(description = "Número de página") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Cantidad de elementos por página") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "ID de la empresa (opcional para SUPERADMIN)") @RequestParam(required = false) UUID companyId,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        UUID targetCompanyId = resolveCompanyId(companyId, currentUser);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        PageResponseDTO<InventoryAlertResponseDTO> response = dashboardService.getInventoryAlerts(targetCompanyId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{companyId}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA')")
    @Operation(summary = "Obtener datos consolidados del Dashboard", description = "Calcula metricas agregadas de ingresos, egresos, cotizaciones y proyectos para la empresa especificada.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Datos del dashboard obtenidos exitosamente", content = @Content(schema = @Schema(implementation = DashboardResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<DashboardResponseDTO> getDashboardData(
            @PathVariable UUID companyId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        UUID targetCompanyId = resolveCompanyId(companyId, currentUser);
        DashboardResponseDTO response = dashboardService.getUnifiedDashboardData(targetCompanyId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{companyId}/seed")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA')")
    @Operation(summary = "Generar datos de prueba para Dashboard", description = "Puebla datos masivos e integrados para pruebas operativas y financieras.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Datos de prueba generados exitosamente"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<Void> seedDashboardData(
            @PathVariable UUID companyId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        UUID targetCompanyId = resolveCompanyId(companyId, currentUser);
        if (projectSeedService != null) {
            projectSeedService.seedProjectsData(targetCompanyId);
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/seed-all")
    @PreAuthorize("hasAnyRole('SUPERADMIN')")
    @Operation(summary = "Poblar datos masivos para todas las empresas", description = "Genera datos masivos reales e integrados de inventario, maquinaria, personal, proyectos, facturas y proveedores en todas las empresas.")
    public ResponseEntity<Void> seedAllCompaniesData() {
        if (projectSeedService != null) {
            projectSeedService.seedAllCompaniesData();
        }
        return ResponseEntity.ok().build();
    }

    private UUID resolveCompanyId(UUID paramCompanyId, UserPrincipal currentUser) {
        boolean isSuperAdmin = currentUser != null && "ROLE_SUPERADMIN".equals(currentUser.getRole());
        UUID targetCompanyId = isSuperAdmin ? (paramCompanyId != null ? paramCompanyId : (currentUser != null ? currentUser.getCompanyId() : null))
                : (currentUser != null ? currentUser.getCompanyId() : paramCompanyId);

        if (targetCompanyId == null) {
            throw new AccessDeniedException("Acceso denegado: Debe especificar la empresa.");
        }
        return targetCompanyId;
    }
}
