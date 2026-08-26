package com.sepisac.backend.dto;

import java.util.UUID;

public class AuthResponseDTO {

    private String token;
    private String type;
    private String email;
    private String username;
    private String role;
    private UUID companyId;

    public AuthResponseDTO() {
    }

    public AuthResponseDTO(String token, String type, String email, String username, String role, UUID companyId) {
        this.token = token;
        this.type = type;
        this.email = email;
        this.username = username;
        this.role = role;
        this.companyId = companyId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
}
