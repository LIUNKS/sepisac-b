package com.sepisac.backend.controller;

import com.sepisac.backend.dto.AuthResponseDTO;
import com.sepisac.backend.dto.AuthResult;
import com.sepisac.backend.dto.ErrorResponseDTO;
import com.sepisac.backend.dto.LoginRequestDTO;
import com.sepisac.backend.security.AuthCookieProvider;
import com.sepisac.backend.security.UserPrincipal;
import com.sepisac.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Endpoints para inicio de sesión, cierre de sesión y consulta del usuario autenticado mediante cookies HttpOnly")
public class AuthController {

    private final AuthService authService;
    private final AuthCookieProvider authCookieProvider;

    public AuthController(AuthService authService, AuthCookieProvider authCookieProvider) {
        this.authService = authService;
        this.authCookieProvider = authCookieProvider;
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Iniciar sesión", description = "Autentica las credenciales del usuario, emite una cookie HttpOnly con el token JWT y retorna los datos del perfil.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticación exitosa. Retorna la cookie en Set-Cookie y los datos del usuario en el cuerpo.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Petición inválida (formato de correo erróneo o campos obligatorios vacíos).", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Credenciales incorrectas o cuenta de usuario inactiva.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        AuthResult authResult = authService.login(request);
        ResponseCookie cookie = authCookieProvider.createAuthCookie(authResult.token());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(authResult.responseDTO());
    }

    @PostMapping("/logout")
    @SecurityRequirements
    @Operation(summary = "Cerrar sesión", description = "Elimina la sesión del usuario invalidando y limpiando la cookie HttpOnly.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesión cerrada con éxito. La cookie de autenticación ha sido eliminada.")
    })
    public ResponseEntity<Void> logout() {
        ResponseCookie cleanCookie = authCookieProvider.createCleanAuthCookie();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cleanCookie.toString())
                .build();
    }

    @GetMapping("/me")
    @Operation(summary = "Obtener usuario actual", description = "Retorna la información del usuario autenticado en la sesión actual.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Datos del usuario obtenidos con éxito.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado o sesión no encontrada.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<AuthResponseDTO> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BadCredentialsException("No autenticado o sesión no encontrada");
        }

        AuthResponseDTO responseDTO = new AuthResponseDTO(
                principal.getEmail(),
                principal.getUsername(),
                principal.getRole(),
                principal.getCompanyId()
        );

        return ResponseEntity.ok(responseDTO);
    }
}
