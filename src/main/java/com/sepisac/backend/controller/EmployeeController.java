package com.sepisac.backend.controller;

import com.sepisac.backend.dto.EmployeeCreateDTO;
import com.sepisac.backend.dto.EmployeeFilterDTO;
import com.sepisac.backend.dto.EmployeeResponseDTO;
import com.sepisac.backend.dto.EmployeeUpdateDTO;
import com.sepisac.backend.dto.ErrorResponseDTO;
import com.sepisac.backend.dto.PageResponseDTO;
import com.sepisac.backend.security.UserPrincipal;
import com.sepisac.backend.service.EmployeeService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/employees")
@Tag(name = "Employees", description = "Endpoints para la gestión de Personal, Recursos Humanos y Costos de Mano de Obra")
@SecurityRequirement(name = "bearerAuth")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA')")
    @Operation(summary = "Registrar nuevo empleado", description = "Registra un nuevo trabajador operativo con su costo por hora y sueldo.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Empleado registrado exitosamente", content = @Content(schema = @Schema(implementation = EmployeeResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o costo negativo", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<EmployeeResponseDTO> createEmployee(
            @Valid @RequestBody EmployeeCreateDTO requestDTO,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        EmployeeResponseDTO created = employeeService.createEmployee(requestDTO, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA', 'ALMACEN')")
    @Operation(summary = "Listar empleados con paginación y filtros", description = "Obtiene la lista paginada de empleados activos de la empresa.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado paginado de empleados obtenido exitosamente", content = @Content(schema = @Schema(implementation = PageResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<PageResponseDTO<EmployeeResponseDTO>> getEmployees(
            @Parameter(description = "ID de la empresa (solo obligatorio para SUPERADMIN si desea filtrar)") @RequestParam(required = false) UUID companyId,
            @Parameter(description = "Número de página (0..N)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Cantidad de elementos por página") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Búsqueda por nombre o especialidad") @RequestParam(required = false) String search,
            @Parameter(description = "Tipo de contrato (PLANILLA o LOCACION)") @RequestParam(required = false) String contractType,
            @Parameter(description = "Filtro de disponibilidad (true/false)") @RequestParam(required = false) Boolean isAvailable,
            @Parameter(description = "Criterio de ordenamiento (ej. createdAt,desc)") @RequestParam(defaultValue = "createdAt,desc") String sort,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        PageResponseDTO<EmployeeResponseDTO> employees = employeeService.getEmployeesPaged(
                companyId, page, size, search, contractType, isAvailable, sort, currentUser
        );
        return ResponseEntity.ok(employees);
    }

    @PostMapping("/search")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA', 'ALMACEN')")
    @Operation(summary = "Búsqueda avanzada de empleados (Body JSON)", description = "Búsqueda estructurada de empleados con filtros en el cuerpo de la petición.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resultados de búsqueda obtenidos exitosamente", content = @Content(schema = @Schema(implementation = PageResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<PageResponseDTO<EmployeeResponseDTO>> queryEmployees(
            @RequestBody EmployeeFilterDTO filter,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        PageResponseDTO<EmployeeResponseDTO> result = employeeService.queryEmployees(filter, currentUser);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA', 'ALMACEN')")
    @Operation(summary = "Obtener empleado por ID", description = "Obtiene los detalles del empleado especificado por UUID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Empleado encontrado exitosamente", content = @Content(schema = @Schema(implementation = EmployeeResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Empleado no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<EmployeeResponseDTO> getEmployeeById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        EmployeeResponseDTO employee = employeeService.getEmployeeById(id, currentUser);
        return ResponseEntity.ok(employee);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA')")
    @Operation(summary = "Actualizar empleado", description = "Actualiza la información laboral y salarial de un empleado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Empleado actualizado exitosamente", content = @Content(schema = @Schema(implementation = EmployeeResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Empleado no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<EmployeeResponseDTO> updateEmployee(
            @PathVariable UUID id,
            @Valid @RequestBody EmployeeUpdateDTO requestDTO,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        EmployeeResponseDTO updated = employeeService.updateEmployee(id, requestDTO, currentUser);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/toggle-availability")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA')")
    @Operation(summary = "Alternar disponibilidad del empleado", description = "Cambia el estado de disponibilidad del empleado para asignaciones.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Disponibilidad actualizada exitosamente", content = @Content(schema = @Schema(implementation = EmployeeResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Empleado no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<EmployeeResponseDTO> toggleAvailability(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        EmployeeResponseDTO updated = employeeService.toggleAvailability(id, currentUser);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA')")
    @Operation(summary = "Eliminar empleado (Soft Delete)", description = "Realiza la eliminación lógica del empleado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Empleado eliminado lógicamente exitosamente"),
            @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Empleado no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<Void> deleteEmployee(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        employeeService.deleteEmployee(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
