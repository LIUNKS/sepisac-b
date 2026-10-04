package com.sepisac.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UserUpdateDTO {

    private java.util.UUID companyId;

    @NotBlank(message = "El nombre completo es obligatorio")
    private String fullName;

    private String username;

    @NotNull(message = "El rol es obligatorio")
    private Integer roleId;

    public UserUpdateDTO() {
    }

    public UserUpdateDTO(String fullName, String username, Integer roleId) {
        this.fullName = fullName;
        this.username = username;
        this.roleId = roleId;
    }

    
    public java.util.UUID getCompanyId() { return companyId; }
    public void setCompanyId(java.util.UUID companyId) { this.companyId = companyId; }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }
}
