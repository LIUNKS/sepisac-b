package com.sepisac.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Datos del usuario autenticado retornados tras el inicio de sesión")
public class AuthResponseDTO {

    @Schema(description = "Correo electrónico del usuario", example = "usuario@sepisac.com")
    private String email;

    @Schema(description = "Nombre de usuario", example = "johan_admin")
    private String username;

    @Schema(description = "Nombre completo del usuario", example = "Johan Admin")
    private String fullName;

    @Schema(description = "Rol del usuario en el sistema", example = "ROLE_ADMIN_EMPRESA")
    private String role;

    @Schema(description = "Identificador único de la empresa asociada", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID companyId;

    public AuthResponseDTO() {
    }

    public AuthResponseDTO(String email, String username, String fullName, String role, UUID companyId) {
        this.email = email;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.companyId = companyId;
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

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
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
}
