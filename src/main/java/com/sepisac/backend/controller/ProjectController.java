package com.sepisac.backend.controller;

import com.sepisac.backend.dto.*;
import com.sepisac.backend.service.ProjectExecutionService;
import com.sepisac.backend.service.ProjectSeedService;
import com.sepisac.backend.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/projects", "/api/projects"})
@Tag(name = "Projects & Operations", description = "Endpoints para la gestión, orquestación y ciclo de vida de Proyectos y Operaciones")
@SecurityRequirement(name = "bearerAuth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectSeedService projectSeedService;
    private final ProjectExecutionService projectExecutionService;

    @Autowired
    public ProjectController(
            ProjectService projectService,
            ProjectSeedService projectSeedService,
            ProjectExecutionService projectExecutionService) {
        this.projectService = projectService;
        this.projectSeedService = projectSeedService;
        this.projectExecutionService = projectExecutionService;
    }

    @PostMapping("/from-quotation/{quotationId}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA')")
    @Operation(summary = "Crear proyecto desde cotización aprobada (SLA Crítico)", 
               description = "Lee una cotización aprobada, inserta la cabecera del proyecto, prepara el entorno operativo y retorna los detalles del proyecto creado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Proyecto creado exitosamente a partir de la cotización", content = @Content(schema = @Schema(implementation = ProjectResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Cotización no encontrada", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Regla de negocio: la cotización no está en estado APROBADA", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<ProjectResponseDTO> createProjectFromQuotation(
            @Parameter(description = "ID de la cotización aprobada") @PathVariable UUID quotationId) {
        ProjectResponseDTO response = projectExecutionService.createProjectFromQuotation(quotationId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA', 'ALMACEN', 'TECNICO')")
    @Operation(summary = "Actualizar estado del proyecto", 
               description = "Transiciona el estado del proyecto a lo largo del flujo operativo (PENDIENTE → EN_PROCESO → COMPLETADO / CANCELADO).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado del proyecto actualizado exitosamente", content = @Content(schema = @Schema(implementation = ProjectResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Estado no válido o formato erróneo", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Proyecto no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<ProjectResponseDTO> updateProjectStatus(
            @Parameter(description = "ID del proyecto") @PathVariable UUID id,
            @Valid @RequestBody ProjectStatusUpdateDTO updateDTO) {
        ProjectResponseDTO response = projectExecutionService.updateProjectStatus(id, updateDTO);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/seed/{companyId}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA')")
    @Operation(summary = "Poblar proyectos demo para empresa", description = "Genera datos semilla de proyectos para fines de prueba y demostración.")
    public ResponseEntity<Void> seedProjects(@PathVariable UUID companyId) {
        projectSeedService.seedProjectsData(companyId);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA', 'ALMACEN', 'TECNICO')")
    @Operation(summary = "Listar proyectos con paginación y filtros", description = "Obtiene la lista paginada de proyectos filtrados por empresa, estado o búsqueda.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado paginado obtenido exitosamente", content = @Content(schema = @Schema(implementation = PageResponseDTO.class)))
    })
    public ResponseEntity<PageResponseDTO<ProjectResponseDTO>> getProjects(
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") String direction) {
        
        PageResponseDTO<ProjectResponseDTO> response = projectService.getProjects(
                companyId, status, search, page, size, sort, direction);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA', 'ALMACEN', 'TECNICO')")
    @Operation(summary = "Obtener proyecto por ID", description = "Recupera los detalles de un proyecto específico.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Proyecto encontrado", content = @Content(schema = @Schema(implementation = ProjectResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Proyecto no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<ProjectResponseDTO> getProjectById(@PathVariable UUID id) {
        ProjectResponseDTO response = projectService.getProjectById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA')")
    @Operation(summary = "Crear proyecto manualmente", description = "Registra un nuevo proyecto con datos manuales directos.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Proyecto registrado", content = @Content(schema = @Schema(implementation = ProjectResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<ProjectResponseDTO> createProject(@Valid @RequestBody ProjectCreateDTO createDTO) {
        ProjectResponseDTO response = projectService.createProject(createDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA')")
    @Operation(summary = "Actualizar datos generales de proyecto", description = "Modifica los campos generales de un proyecto existente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Proyecto actualizado", content = @Content(schema = @Schema(implementation = ProjectResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Proyecto no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<ProjectResponseDTO> updateProject(
            @PathVariable UUID id,
            @Valid @RequestBody ProjectUpdateDTO updateDTO) {
        ProjectResponseDTO response = projectService.updateProject(id, updateDTO);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA')")
    @Operation(summary = "Eliminar proyecto (Soft Delete)", description = "Marca un proyecto como eliminado sin perder trazabilidad.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Proyecto eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Proyecto no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<Void> deleteProject(@PathVariable UUID id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }
}
