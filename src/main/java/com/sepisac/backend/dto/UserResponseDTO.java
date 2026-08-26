package com.sepisac.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class UserResponseDTO {

    private UUID id;
    private UUID companyId;
    private String companyName;
    private Integer roleId;
    private String roleName;
    private String username;
    private String email;
    private String fullName;
    private Boolean isActive;
    private OffsetDateTime createdAt;

    public UserResponseDTO() {
    }

    public UserResponseDTO(UUID id, UUID companyId, String companyName, Integer roleId, String roleName,
                           String username, String email, String fullName, Boolean isActive, OffsetDateTime createdAt) {
        this.id = id;
        this.companyId = companyId;
        this.companyName = companyName;
        this.roleId = roleId;
        this.roleName = roleName;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
