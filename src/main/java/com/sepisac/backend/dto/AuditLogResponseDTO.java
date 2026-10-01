package com.sepisac.backend.dto;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public class AuditLogResponseDTO {

    private UUID id;
    private AuditLogUserDTO user;
    private String action;
    private String moduleAffected;
    private UUID entityId;
    private String description;
    private Map<String, Object> oldValues;
    private Map<String, Object> newValues;
    private OffsetDateTime createdAt;

    public AuditLogResponseDTO() {
    }

    public AuditLogResponseDTO(UUID id,
                               AuditLogUserDTO user,
                               String action,
                               String moduleAffected,
                               UUID entityId,
                               String description,
                               Map<String, Object> oldValues,
                               Map<String, Object> newValues,
                               OffsetDateTime createdAt) {
        this.id = id;
        this.user = user;
        this.action = action;
        this.moduleAffected = moduleAffected;
        this.entityId = entityId;
        this.description = description;
        this.oldValues = oldValues;
        this.newValues = newValues;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public AuditLogUserDTO getUser() {
        return user;
    }

    public void setUser(AuditLogUserDTO user) {
        this.user = user;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getModuleAffected() {
        return moduleAffected;
    }

    public void setModuleAffected(String moduleAffected) {
        this.moduleAffected = moduleAffected;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getOldValues() {
        return oldValues;
    }

    public void setOldValues(Map<String, Object> oldValues) {
        this.oldValues = oldValues;
    }

    public Map<String, Object> getNewValues() {
        return newValues;
    }

    public void setNewValues(Map<String, Object> newValues) {
        this.newValues = newValues;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
