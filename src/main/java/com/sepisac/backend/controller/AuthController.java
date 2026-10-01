package com.sepisac.backend.controller;

import com.sepisac.backend.dto.AuthResponseDTO;
import com.sepisac.backend.dto.AuthResult;
import com.sepisac.backend.dto.ErrorResponseDTO;
import com.sepisac.backend.dto.LoginRequestDTO;
import com.sepisac.backend.dto.Toggle2FaResponseDTO;
import com.sepisac.backend.dto.Verify2FaRequestDTO;
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
@Tag(name = "Autenticación", description = "Endpoints para inicio de sesión, verificación 2FA, cierre de sesión y gestión de sesión mediante cookies HttpOnly")
public class AuthController {

    private final AuthService authService;
    private final AuthCookieProvider authCookieProvider;

    public AuthController(AuthService authService, AuthCookieProvider authCookieProvider) {
        this.authService = authService;
        this.authCookieProvider = authCookieProvider;
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Iniciar sesión", description = "Autentica credenciales. Si 2FA está deshabilitado emite cookie HttpOnly; si está habilitado solicita código.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticación exitosa o requerimiento de 2FA.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Petición inválida (formato de correo erróneo o campos obligatorios vacíos).", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Credenciales incorrectas o cuenta de usuario inactiva.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        AuthResult authResult = authService.login(request);
        if (authResult.token() == null) {
            return ResponseEntity.ok(authResult.responseDTO());
        }

        ResponseCookie cookie = authCookieProvider.createAuthCookie(authResult.token());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(authResult.responseDTO());
    }

    @PostMapping("/verify-2fa")
    @SecurityRequirements
    @Operation(summary = "Verificar código 2FA", description = "Valida el código de 6 dígitos enviado por correo para completar la autenticación y emitir la cookie de sesión.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Verificación 2FA exitosa. Retorna cookie de sesión y datos del perfil.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Formato de correo o código inválido.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Código incorrecto, expirado o usuario no autenticado.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "El usuario no tiene la autenticación 2FA activada.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<AuthResponseDTO> verify2Fa(@Valid @RequestBody Verify2FaRequestDTO request) {
        AuthResult authResult = authService.verify2Fa(request);
        ResponseCookie cookie = authCookieProvider.createAuthCookie(authResult.token());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(authResult.responseDTO());
    }

    @PostMapping("/2fa/toggle")
    @Operation(summary = "Alternar estado de 2FA", description = "Activa o desactiva el segundo factor de autenticación para el usuario en sesión actual.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado 2FA alternado con éxito.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Toggle2FaResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado o sesión no encontrada.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<Toggle2FaResponseDTO> toggle2Fa() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BadCredentialsException("No autenticado o sesión no encontrada");
        }

        Toggle2FaResponseDTO response = authService.toggle2Fa(principal.getId());
        return ResponseEntity.ok(response);
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
                principal.getFullName(),
                principal.getRole(),
                principal.getCompanyId()
        );
        responseDTO.setTwoFactorEnabled(principal.isTwoFactorEnabled());

        return ResponseEntity.ok(responseDTO);
    }
}
