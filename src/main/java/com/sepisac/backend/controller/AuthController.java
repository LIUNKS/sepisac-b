package com.sepisac.backend.controller;

import com.sepisac.backend.dto.AuthResponseDTO;
import com.sepisac.backend.dto.ErrorResponseDTO;
import com.sepisac.backend.dto.LoginRequestDTO;
import com.sepisac.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Endpoints para inicio de sesión y gestión de credenciales JWT")
public class AuthController {

        private final AuthService authService;

        public AuthController(AuthService authService) {
                this.authService = authService;
        }

        @PostMapping("/login")
        @SecurityRequirements
        @Operation(summary = "Iniciar sesión", description = "Autentica las credenciales del usuario y genera un token JWT stateless con el claim del tenant (company_id).")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Autenticación exitosa. Retorna el token JWT y los datos del usuario.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponseDTO.class))),
                        @ApiResponse(responseCode = "400", description = "Petición inválida (formato de correo erróneo o campos obligatorios vacíos).", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
                        @ApiResponse(responseCode = "401", description = "Credenciales incorrectas o cuenta de usuario inactiva.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
                AuthResponseDTO response = authService.login(request);
                return ResponseEntity.ok(response);
        }
}
