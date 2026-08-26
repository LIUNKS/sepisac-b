package com.sepisac.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class UserCreateDTO {

    private UUID companyId;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "Formato de correo electrónico inválido")
    private String email;

    private String username;

    @NotBlank(message = "El nombre completo es obligatorio")
    private String fullName;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String password;

    @NotNull(message = "El rol es obligatorio")
    private Integer roleId;

    public UserCreateDTO() {
    }

    public UserCreateDTO(UUID companyId, String email, String username, String fullName, String password, Integer roleId) {
        this.companyId = companyId;
        this.email = email;
        this.username = username;
        this.fullName = fullName;
        this.password = password;
        this.roleId = roleId;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public void setCompanyId(UUID companyId) {
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }
}
