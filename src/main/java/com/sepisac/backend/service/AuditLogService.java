package com.sepisac.backend.service;

import com.sepisac.backend.dto.AuditLogResponseDTO;
import com.sepisac.backend.dto.AuditLogUserDTO;
import com.sepisac.backend.dto.PageResponseDTO;
import com.sepisac.backend.exception.ResourceNotFoundException;
import com.sepisac.backend.model.AuditLogEntity;
import com.sepisac.backend.model.UserEntity;
import com.sepisac.backend.repository.AuditLogRepository;
import com.sepisac.backend.repository.CompanyRepository;
import com.sepisac.backend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AuditLogService {

    public static final Set<String> ALLOWED_MODULES = Set.of(
            "QUOTATIONS",
            "COMMERCIAL_QUOTATIONS",
            "PROJECTS",
            "INVENTORY",
            "PURCHASE_ORDERS",
            "SUPPLIERS",
            "FINANCE",
            "USERS",
            "COMPANY",
            "AUTH",
            "PROJECT_EXECUTION"
    );

    public static final Set<String> ALLOWED_ACTIONS = Set.of(
            "CREATE",
            "UPDATE",
            "DELETE",
            "APPROVE",
            "REJECT",
            "CANCEL",
            "STATUS_UPDATE",
            "LOGIN",
            "LOGOUT",
            "INVOICE_CREATED",
            "PAYMENT_REGISTERED",
            "CONSUME",
            "RECEIVE"
    );

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password",
            "password_hash",
            "passwordhash",
            "two_factor_code",
            "twofactorcode",
            "token",
            "secret"
    );

    private final AuditLogRepository auditLogRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    public AuditLogService(AuditLogRepository auditLogRepository,
                           CompanyRepository companyRepository,
                           UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
    }

    public void log(UUID companyId, UUID userId, String action, String moduleAffected, String description) {
        log(companyId, userId, action, moduleAffected, null, description, null, null);
    }

    public void log(UUID companyId, UUID userId, String action, String moduleAffected, UUID entityId,
                    String description, Map<String, Object> oldValues, Map<String, Object> newValues) {
        AuditLogEntity auditLog = new AuditLogEntity();
        if (companyId != null) {
            companyRepository.findById(companyId).ifPresent(auditLog::setCompany);
        }
        if (userId != null) {
            userRepository.findById(userId).ifPresent(auditLog::setUser);
        }
        auditLog.setAction(action);
        auditLog.setModuleAffected(moduleAffected);
        auditLog.setEntityId(entityId);
        auditLog.setDescription(description);
        auditLog.setOldValues(sanitizeSensitiveFields(oldValues));
        auditLog.setNewValues(sanitizeSensitiveFields(newValues));
        auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<AuditLogResponseDTO> getAuditLogs(
            UUID companyId,
            String module,
            UUID userId,
            String action,
            UUID entityId,
            LocalDate from,
            LocalDate to,
            Pageable pageable) {

        if (companyId == null) {
            throw new IllegalArgumentException("VALIDATION_ERROR: companyId es requerido");
        }

        if (module != null && !module.isBlank()) {
            String normalizedModule = module.trim().toUpperCase();
            if (!ALLOWED_MODULES.contains(normalizedModule)) {
                throw new IllegalArgumentException("VALIDATION_ERROR: Módulo no válido: " + module);
            }
            module = normalizedModule;
        } else {
            module = null;
        }

        if (action != null && !action.isBlank()) {
            String normalizedAction = action.trim().toUpperCase();
            if (!ALLOWED_ACTIONS.contains(normalizedAction)) {
                throw new IllegalArgumentException("VALIDATION_ERROR: Acción no válida: " + action);
            }
            action = normalizedAction;
        } else {
            action = null;
        }

        if (from != null && to != null) {
            if (from.isAfter(to)) {
                throw new IllegalArgumentException("VALIDATION_ERROR: 'from' date must be before or equal to 'to' date");
            }
            if (ChronoUnit.DAYS.between(from, to) > 366) {
                throw new IllegalArgumentException("VALIDATION_ERROR: Date range cannot exceed 366 days");
            }
        }

        // RN-D16: Max page size limited to 100
        int pageSize = pageable != null ? pageable.getPageSize() : 20;
        if (pageSize > 100) {
            pageSize = 100;
        }
        int pageNumber = pageable != null ? Math.max(0, pageable.getPageNumber()) : 0;
        Sort sort = (pageable != null && pageable.getSort().isSorted())
                ? pageable.getSort()
                : Sort.by(Sort.Direction.DESC, "createdAt");

        Pageable effectivePageable = PageRequest.of(pageNumber, pageSize, sort);

        OffsetDateTime fromOffset = from != null ? from.atStartOfDay().atOffset(ZoneOffset.UTC) : null;
        OffsetDateTime toOffset = to != null ? to.atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC) : null;

        Page<AuditLogEntity> pagedResult = auditLogRepository.findByCompanyIdWithFilters(
                companyId, module, userId, action, entityId, fromOffset, toOffset, effectivePageable
        );

        List<AuditLogResponseDTO> dtoList = pagedResult.getContent().stream()
                .map(this::mapToDTO)
                .toList();

        return new PageResponseDTO<>(
                dtoList,
                pagedResult.getNumber(),
                pagedResult.getSize(),
                pagedResult.getTotalElements(),
                pagedResult.getTotalPages(),
                pagedResult.isFirst(),
                pagedResult.isLast()
        );
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponseDTO> getEntityAuditTrail(UUID companyId, String module, UUID entityId) {
        if (companyId == null) {
            throw new IllegalArgumentException("VALIDATION_ERROR: companyId es requerido");
        }
        if (entityId == null) {
            throw new IllegalArgumentException("VALIDATION_ERROR: entityId es requerido");
        }
        if (module != null && !module.isBlank()) {
            String normalizedModule = module.trim().toUpperCase();
            if (!ALLOWED_MODULES.contains(normalizedModule)) {
                throw new IllegalArgumentException("VALIDATION_ERROR: Módulo no válido: " + module);
            }
            module = normalizedModule;
        }

        List<AuditLogEntity> logs = auditLogRepository.findByCompanyIdAndModuleAffectedAndEntityIdOrderByCreatedAtAsc(
                companyId, module, entityId
        );

        if (logs.isEmpty()) {
            throw new ResourceNotFoundException("AUDIT_ENTITY_NOT_FOUND: No se encontraron registros de auditoría para la entidad " + entityId);
        }

        return logs.stream().map(this::mapToDTO).toList();
    }

    public AuditLogResponseDTO mapToDTO(AuditLogEntity entity) {
        AuditLogUserDTO userDTO = null;
        UserEntity user = entity.getUser();
        if (user != null && !Boolean.TRUE.equals(user.getIsDeleted())) {
            userDTO = new AuditLogUserDTO(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getFullName()
            );
        }

        return new AuditLogResponseDTO(
                entity.getId(),
                userDTO,
                entity.getAction(),
                entity.getModuleAffected(),
                entity.getEntityId(),
                entity.getDescription(),
                sanitizeSensitiveFields(entity.getOldValues()),
                sanitizeSensitiveFields(entity.getNewValues()),
                entity.getCreatedAt()
        );
    }

    public Map<String, Object> sanitizeSensitiveFields(Map<String, Object> values) {
        if (values == null) {
            return null;
        }
        Map<String, Object> sanitized = new HashMap<>();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (entry.getKey() != null && !SENSITIVE_KEYS.contains(entry.getKey().toLowerCase())) {
                sanitized.put(entry.getKey(), entry.getValue());
            }
        }
        return sanitized;
    }
}
