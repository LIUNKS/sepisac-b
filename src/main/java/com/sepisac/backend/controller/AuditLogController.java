package com.sepisac.backend.controller;

import com.sepisac.backend.dto.AuditLogResponseDTO;
import com.sepisac.backend.dto.ErrorResponseDTO;
import com.sepisac.backend.dto.PageResponseDTO;
import com.sepisac.backend.security.UserPrincipal;
import com.sepisac.backend.service.AuditLogService;
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
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit-logs")
@Tag(name = "Audit Logs", description = "Endpoints para la consulta de Logs de Auditoría y Trazabilidad Gerencial")
@SecurityRequirement(name = "bearerAuth")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA')")
    @Operation(summary = "Listar registros de auditoría", description = "Consulta paginada de registros de auditoría con filtros por módulo, usuario, acción, entidad y rango de fechas.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado de auditoría obtenido exitosamente", content = @Content(schema = @Schema(implementation = PageResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Filtros inválidos o módulo/acción no permitido", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<PageResponseDTO<AuditLogResponseDTO>> getAuditLogs(
            @Parameter(description = "Módulo afectado (ej. QUOTATIONS, PROJECTS, INVENTORY)") @RequestParam(required = false) String module,
            @Parameter(description = "ID del usuario que realizó la acción") @RequestParam(required = false) UUID userId,
            @Parameter(description = "Acción realizada (ej. CREATE, UPDATE, DELETE, APPROVE)") @RequestParam(required = false) String action,
            @Parameter(description = "ID de la entidad afectada") @RequestParam(required = false) UUID entityId,
            @Parameter(description = "Fecha inicial (YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Fecha final (YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "Número de página (0..N)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Cantidad de elementos por página (máx 100)") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Criterio de ordenamiento (ej. createdAt,desc)") @RequestParam(defaultValue = "createdAt,desc") String sort,
            @Parameter(description = "ID de la empresa (opcional para SUPERADMIN)") @RequestParam(required = false) UUID companyId,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        UUID targetCompanyId = resolveCompanyId(companyId, currentUser);
        Pageable pageable = createPageable(page, size, sort);

        PageResponseDTO<AuditLogResponseDTO> response = auditLogService.getAuditLogs(
                targetCompanyId, module, userId, action, entityId, from, to, pageable
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{module}/{entityId}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA')")
    @Operation(summary = "Trazabilidad completa de una entidad", description = "Obtiene el historial cronológico ascendente de auditoría de una entidad específica.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Trazabilidad obtenida exitosamente"),
            @ApiResponse(responseCode = "400", description = "Módulo inválido", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Entidad no encontrada o sin registros de auditoría", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<List<AuditLogResponseDTO>> getEntityAuditTrail(
            @PathVariable String module,
            @PathVariable UUID entityId,
            @Parameter(description = "ID de la empresa (opcional para SUPERADMIN)") @RequestParam(required = false) UUID companyId,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        UUID targetCompanyId = resolveCompanyId(companyId, currentUser);
        List<AuditLogResponseDTO> response = auditLogService.getEntityAuditTrail(targetCompanyId, module, entityId);
        return ResponseEntity.ok(response);
    }

    @RequestMapping(method = {RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE})
    @Operation(summary = "Operación no permitida", description = "Los registros de auditoría son inmutables. Bloqueado con 405 Method Not Allowed.")
    @ApiResponse(responseCode = "405", description = "Método HTTP no permitido para logs de auditoría")
    public ResponseEntity<Void> rejectModificationsRoot() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }

    @RequestMapping(value = "/**", method = {RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE})
    @Operation(summary = "Operación no permitida", description = "Los registros de auditoría son inmutables. Bloqueado con 405 Method Not Allowed.")
    @ApiResponse(responseCode = "405", description = "Método HTTP no permitido para logs de auditoría")
    public ResponseEntity<Void> rejectModificationsSubpaths() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
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

    private Pageable createPageable(int page, int size, String sortStr) {
        int pageNum = Math.max(0, page);
        int pageSize = Math.min(Math.max(1, size), 100);

        Sort.Direction direction = Sort.Direction.DESC;
        String property = "createdAt";

        if (sortStr != null && !sortStr.trim().isEmpty()) {
            String[] parts = sortStr.split(",");
            property = parts[0].trim();
            if (parts.length > 1 && parts[1].trim().equalsIgnoreCase("asc")) {
                direction = Sort.Direction.ASC;
            }
        }

        return PageRequest.of(pageNum, pageSize, Sort.by(direction, property));
    }
}
