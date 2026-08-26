package com.sepisac.backend.controller;

import com.sepisac.backend.dto.CompanyCreateDTO;
import com.sepisac.backend.dto.CompanyFilterDTO;
import com.sepisac.backend.dto.CompanyResponseDTO;
import com.sepisac.backend.dto.ErrorResponseDTO;
import com.sepisac.backend.dto.PageResponseDTO;
import com.sepisac.backend.security.UserPrincipal;
import com.sepisac.backend.service.CompanyService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/companies")
@Tag(name = "Companies", description = "Endpoints para la gestión de Empresas (Tenants)")
@SecurityRequirement(name = "bearerAuth")
public class CompanyController {

        private final CompanyService companyService;

        public CompanyController(CompanyService companyService) {
                this.companyService = companyService;
        }

        @PostMapping
        @PreAuthorize("hasRole('SUPERADMIN')")
        @Operation(summary = "Crear nueva empresa", description = "Crea un nuevo tenant en el sistema. Requiere rol SUPERADMIN.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Empresa creada exitosamente", content = @Content(schema = @Schema(implementation = CompanyResponseDTO.class))),
                        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                        @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                        @ApiResponse(responseCode = "409", description = "RUC ya registrado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        public ResponseEntity<CompanyResponseDTO> createCompany(
                        @Valid @RequestBody CompanyCreateDTO requestDTO,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                CompanyResponseDTO createdCompany = companyService.createCompany(requestDTO, currentUser);
                return ResponseEntity.status(HttpStatus.CREATED).body(createdCompany);
        }

        @GetMapping
        @PreAuthorize("hasRole('SUPERADMIN')")
        @Operation(summary = "Listar empresas con paginación y filtros", description = "Obtiene la lista paginada de empresas. Soporta búsqueda por RUC/Razón Social y filtro por estado. Requiere rol SUPERADMIN.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Listado paginado de empresas obtenido exitosamente", content = @Content(schema = @Schema(implementation = PageResponseDTO.class))),
                        @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        public ResponseEntity<PageResponseDTO<CompanyResponseDTO>> getCompanies(
                        @Parameter(description = "Número de página (0..N)") @RequestParam(defaultValue = "0") int page,
                        @Parameter(description = "Cantidad de elementos por página") @RequestParam(defaultValue = "10") int size,
                        @Parameter(description = "Término de búsqueda (RUC o Razón Social)") @RequestParam(required = false) String search,
                        @Parameter(description = "Estado de suscripción (ACTIVE, SUSPENDED, TRIAL)") @RequestParam(required = false) String subscriptionStatus,
                        @Parameter(description = "Criterio de ordenamiento (ej. createdAt,desc)") @RequestParam(defaultValue = "createdAt,desc") String sort) {
                PageResponseDTO<CompanyResponseDTO> companies = companyService.getCompaniesPaged(page, size, search,
                                subscriptionStatus, sort);
                return ResponseEntity.ok(companies);
        }

        @PostMapping("/search")
        @PreAuthorize("hasRole('SUPERADMIN')")
        @Operation(summary = "Búsqueda avanzada de empresas (Filtros en Body JSON)", description = "Búsqueda avanzada enviando criterios estructurados en el cuerpo JSON. Requiere rol SUPERADMIN.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Resultados de búsqueda obtenidos exitosamente", content = @Content(schema = @Schema(implementation = PageResponseDTO.class))),
                        @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        public ResponseEntity<PageResponseDTO<CompanyResponseDTO>> queryCompanies(
                        @RequestBody CompanyFilterDTO filter,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                PageResponseDTO<CompanyResponseDTO> result = companyService.queryCompanies(filter, currentUser);
                return ResponseEntity.ok(result);
        }

        @GetMapping("/{id}")
        @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA')")
        @Operation(summary = "Obtener empresa por ID", description = "Obtiene el detalle de una empresa por su UUID.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Empresa encontrada exitosamente", content = @Content(schema = @Schema(implementation = CompanyResponseDTO.class))),
                        @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                        @ApiResponse(responseCode = "404", description = "Empresa no encontrada", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        public ResponseEntity<CompanyResponseDTO> getCompanyById(
                        @PathVariable UUID id,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                CompanyResponseDTO company = companyService.getCompanyById(id, currentUser);
                return ResponseEntity.ok(company);
        }
}
