package com.sepisac.backend.controller;

import com.sepisac.backend.dto.ErrorResponseDTO;
import com.sepisac.backend.dto.PageResponseDTO;
import com.sepisac.backend.dto.UserCreateDTO;
import com.sepisac.backend.dto.UserFilterDTO;
import com.sepisac.backend.dto.UserResponseDTO;
import com.sepisac.backend.dto.UserUpdateDTO;
import com.sepisac.backend.security.UserPrincipal;
import com.sepisac.backend.service.UserService;
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
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Endpoints para la gestión de Usuarios, Paginación y Búsqueda")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

        private final UserService userService;

        public UserController(UserService userService) {
                this.userService = userService;
        }

        @PostMapping
        @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA')")
        @Operation(summary = "Crear nuevo usuario", description = "Registra un nuevo usuario en la empresa. Los administradores de empresa no pueden asignar rol SUPERADMIN.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Usuario creado exitosamente", content = @Content(schema = @Schema(implementation = UserResponseDTO.class))),
                        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                        @ApiResponse(responseCode = "403", description = "Permiso denegado / Intento de escalada de privilegios", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                        @ApiResponse(responseCode = "409", description = "Correo electrónico o nombre de usuario duplicado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        public ResponseEntity<UserResponseDTO> createUser(
                        @Valid @RequestBody UserCreateDTO requestDTO,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                UserResponseDTO createdUser = userService.createUser(requestDTO, currentUser);
                return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        }

        @GetMapping("/company/{companyId}")
        @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA')")
        @Operation(summary = "Listar usuarios por empresa con paginación y filtros", description = "Obtiene la lista paginada de usuarios de una empresa. Soporta búsqueda por nombre/correo/username y filtros por estado activo/inactivo o rol.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Lista paginada de usuarios obtenida exitosamente", content = @Content(schema = @Schema(implementation = PageResponseDTO.class))),
                        @ApiResponse(responseCode = "403", description = "Permiso denegado para consultar otra empresa", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        public ResponseEntity<PageResponseDTO<UserResponseDTO>> getUsersByCompany(
                        @PathVariable UUID companyId,
                        @Parameter(description = "Número de página (0..N)") @RequestParam(defaultValue = "0") int page,
                        @Parameter(description = "Cantidad de elementos por página") @RequestParam(defaultValue = "10") int size,
                        @Parameter(description = "Búsqueda por nombre, correo o username") @RequestParam(required = false) String search,
                        @Parameter(description = "Filtro de estado activo (true/false)") @RequestParam(required = false) Boolean isActive,
                        @Parameter(description = "Filtro por ID de rol") @RequestParam(required = false) Integer roleId,
                        @Parameter(description = "Criterio de ordenamiento (ej. createdAt,desc)") @RequestParam(defaultValue = "createdAt,desc") String sort,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                PageResponseDTO<UserResponseDTO> users = userService.getUsersByCompanyPaged(
                                companyId, page, size, search, isActive, roleId, sort, currentUser);
                return ResponseEntity.ok(users);
        }

        @PostMapping("/search")
        @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA')")
        @Operation(summary = "Búsqueda avanzada de usuarios (Filtros en Body JSON)", description = "Búsqueda avanzada enviando criterios estructurados en el cuerpo JSON.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Resultados de búsqueda de usuarios obtenidos exitosamente", content = @Content(schema = @Schema(implementation = PageResponseDTO.class))),
                        @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        public ResponseEntity<PageResponseDTO<UserResponseDTO>> queryUsers(
                        @RequestBody UserFilterDTO filter,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                PageResponseDTO<UserResponseDTO> result = userService.queryUsers(filter, currentUser);
                return ResponseEntity.ok(result);
        }

        @PutMapping("/{id}")
        @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA')")
        @Operation(summary = "Actualizar usuario", description = "Actualiza los datos (nombre completo, username, rol) de un usuario.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Usuario actualizado exitosamente", content = @Content(schema = @Schema(implementation = UserResponseDTO.class))),
                        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                        @ApiResponse(responseCode = "403", description = "Permiso denegado / Intento de escalada de privilegios", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                        @ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                        @ApiResponse(responseCode = "409", description = "Nombre de usuario duplicado en la empresa", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        public ResponseEntity<UserResponseDTO> updateUser(
                        @PathVariable UUID id,
                        @Valid @RequestBody UserUpdateDTO requestDTO,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                UserResponseDTO updatedUser = userService.updateUser(id, requestDTO, currentUser);
                return ResponseEntity.ok(updatedUser);
        }

        @PatchMapping("/{id}/toggle-status")
        @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA')")
        @Operation(summary = "Alternar estado activo/inactivo del usuario", description = "Activa o desactiva la cuenta de un usuario.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Estado del usuario actualizado exitosamente", content = @Content(schema = @Schema(implementation = UserResponseDTO.class))),
                        @ApiResponse(responseCode = "403", description = "Permiso denegado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                        @ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        public ResponseEntity<UserResponseDTO> toggleUserStatus(
                        @PathVariable UUID id,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                UserResponseDTO updatedUser = userService.toggleUserStatus(id, currentUser);
                return ResponseEntity.ok(updatedUser);
        }
}
