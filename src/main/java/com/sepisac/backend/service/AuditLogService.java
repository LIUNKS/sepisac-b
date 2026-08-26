package com.sepisac.backend.service;

import com.sepisac.backend.model.AuditLogEntity;
import com.sepisac.backend.repository.AuditLogRepository;
import com.sepisac.backend.repository.CompanyRepository;
import com.sepisac.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuditLogService {

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
        AuditLogEntity auditLog = new AuditLogEntity();
        if (companyId != null) {
            companyRepository.findById(companyId).ifPresent(auditLog::setCompany);
        }
        if (userId != null) {
            userRepository.findById(userId).ifPresent(auditLog::setUser);
        }
        auditLog.setAction(action);
        auditLog.setModuleAffected(moduleAffected);
        auditLog.setDescription(description);
        auditLogRepository.save(auditLog);
    }
}
