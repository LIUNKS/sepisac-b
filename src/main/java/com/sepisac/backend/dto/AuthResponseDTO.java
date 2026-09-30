package com.sepisac.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Datos del usuario autenticado retornados tras el inicio de sesión")
public class AuthResponseDTO {

    @Schema(description = "Correo electrónico del usuario", example = "usuario@sepisac.com")
    private String email;

    @Schema(description = "Nombre de usuario", example = "johan_admin")
    private String username;

    @Schema(description = "Rol del usuario en el sistema", example = "ROLE_ADMIN_EMPRESA")
    private String role;

    @Schema(description = "Identificador único de la empresa asociada", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID companyId;

    @Schema(description = "Indica si se requiere completar el segundo factor de autenticación", example = "false")
    private Boolean twoFactorRequired;

    @Schema(description = "Indica si el usuario tiene activo el segundo factor de autenticación", example = "false")
    private Boolean twoFactorEnabled;

    public AuthResponseDTO() {
        this.twoFactorRequired = false;
        this.twoFactorEnabled = false;
    }

    public AuthResponseDTO(String email, String username, String role, UUID companyId) {
        this(email, username, role, companyId, false, false);
    }

    public AuthResponseDTO(String email, String username, String role, UUID companyId, Boolean twoFactorRequired) {
        this(email, username, role, companyId, twoFactorRequired, false);
    }

    public AuthResponseDTO(String email, String username, String role, UUID companyId, Boolean twoFactorRequired, Boolean twoFactorEnabled) {
        this.email = email;
        this.username = username;
        this.role = role;
        this.companyId = companyId;
        this.twoFactorRequired = twoFactorRequired != null ? twoFactorRequired : false;
        this.twoFactorEnabled = twoFactorEnabled != null ? twoFactorEnabled : false;
    }

    public static AuthResponseDTO twoFactorRequired(String email) {
        return new AuthResponseDTO(email, null, null, null, true, true);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }

    public Boolean getTwoFactorRequired() {
        return twoFactorRequired;
    }

    public void setTwoFactorRequired(Boolean twoFactorRequired) {
        this.twoFactorRequired = twoFactorRequired;
    }

    public Boolean getTwoFactorEnabled() {
        return twoFactorEnabled;
    }

    public void setTwoFactorEnabled(Boolean twoFactorEnabled) {
        this.twoFactorEnabled = twoFactorEnabled;
    }
}
