package com.sepisac.backend.controller;

import com.sepisac.backend.dto.ErrorResponseDTO;
import com.sepisac.backend.dto.RoleResponseDTO;
import com.sepisac.backend.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@Tag(name = "Roles", description = "Endpoints para el catálogo de Roles del sistema")
@SecurityRequirement(name = "bearerAuth")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @Operation(summary = "Listar catálogo de roles", description = "Retorna todos los roles disponibles en el sistema.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de roles obtenida exitosamente",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = RoleResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<List<RoleResponseDTO>> getAllRoles() {
        List<RoleResponseDTO> roles = roleService.getAllRoles();
        return ResponseEntity.ok(roles);
    }
}
