package com.sepisac.backend.service;

import com.sepisac.backend.dto.MachineryCreateDTO;
import com.sepisac.backend.dto.MachineryFilterDTO;
import com.sepisac.backend.dto.MachineryResponseDTO;
import com.sepisac.backend.dto.MachineryStatusUpdateDTO;
import com.sepisac.backend.dto.MachineryUpdateDTO;
import com.sepisac.backend.dto.PageResponseDTO;
import com.sepisac.backend.exception.DuplicateResourceException;
import com.sepisac.backend.exception.ResourceNotFoundException;
import com.sepisac.backend.model.CompanyEntity;
import com.sepisac.backend.model.MachineryEquipmentEntity;
import com.sepisac.backend.repository.CompanyRepository;
import com.sepisac.backend.repository.MachineryEquipmentRepository;
import com.sepisac.backend.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class MachineryEquipmentService {

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "id", "code", "name", "status", "lastMaintenanceDate", "nextMaintenanceDate", "createdAt", "updatedAt"
    );

    private final MachineryEquipmentRepository machineryRepository;
    private final CompanyRepository companyRepository;
    private final AuditLogService auditLogService;

    public MachineryEquipmentService(MachineryEquipmentRepository machineryRepository,
                                     CompanyRepository companyRepository,
                                     AuditLogService auditLogService) {
        this.machineryRepository = machineryRepository;
        this.companyRepository = companyRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public MachineryResponseDTO createMachinery(MachineryCreateDTO dto, UserPrincipal currentUser) {
        boolean isSuperAdmin = currentUser != null && "ROLE_SUPERADMIN".equals(currentUser.getRole());
        UUID targetCompanyId = isSuperAdmin ? dto.getCompanyId() : (currentUser != null ? currentUser.getCompanyId() : dto.getCompanyId());

        if (targetCompanyId == null) {
            throw new AccessDeniedException("Acceso denegado: Debe especificar la empresa del equipo.");
        }

        if (machineryRepository.existsByCompanyIdAndCode(targetCompanyId, dto.getCode())) {
            throw new DuplicateResourceException("El código de equipo '" + dto.getCode() + "' ya está registrado en esta empresa.");
        }

        CompanyEntity company = companyRepository.findById(targetCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada con id: " + targetCompanyId));

        MachineryEquipmentEntity machinery = new MachineryEquipmentEntity();
        machinery.setCompany(company);
        machinery.setCode(dto.getCode());
        machinery.setName(dto.getName());
        machinery.setStatus((dto.getStatus() != null && !dto.getStatus().trim().isEmpty()) ? dto.getStatus().trim().toUpperCase() : "DISPONIBLE");
        machinery.setLastMaintenanceDate(dto.getLastMaintenanceDate());
        machinery.setNextMaintenanceDate(dto.getNextMaintenanceDate());
        machinery.setIsDeleted(false);

        MachineryEquipmentEntity saved = machineryRepository.save(machinery);

        UUID actorId = currentUser != null ? currentUser.getId() : null;
        auditLogService.log(
                targetCompanyId,
                actorId,
                "CREATE",
                "MACHINERY",
                "Equipo registrado: [" + saved.getCode() + "] " + saved.getName()
        );

        return mapToDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<MachineryResponseDTO> getMachineryByCompany(UUID companyId, UserPrincipal currentUser) {
        validateTenantAccess(companyId, currentUser);
        return machineryRepository.findByCompanyId(companyId).stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<MachineryResponseDTO> getMachineryPaged(
            UUID companyId,
            int page,
            int size,
            String search,
            String status,
            String sort,
            UserPrincipal currentUser) {

        boolean isSuperAdmin = currentUser != null && "ROLE_SUPERADMIN".equals(currentUser.getRole());
        UUID targetCompanyId = isSuperAdmin ? companyId : (currentUser != null ? currentUser.getCompanyId() : companyId);

        if (!isSuperAdmin && targetCompanyId == null) {
            throw new AccessDeniedException("Acceso denegado: Debe especificar la empresa.");
        }

        if (targetCompanyId != null) {
            validateTenantAccess(targetCompanyId, currentUser);
        }

        Pageable pageable = createPageable(page, size, sort);
        String searchParam = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String statusParam = (status != null && !status.trim().isEmpty()) ? status.trim().toUpperCase() : null;

        Page<MachineryEquipmentEntity> pageResult;
        if (targetCompanyId != null) {
            pageResult = machineryRepository.findByCompanyIdWithFilters(targetCompanyId, statusParam, searchParam, pageable);
        } else {
            pageResult = machineryRepository.findAllWithFilters(null, statusParam, searchParam, pageable);
        }

        List<MachineryResponseDTO> content = pageResult.getContent().stream()
                .map(this::mapToDTO)
                .toList();

        return new PageResponseDTO<>(
                content,
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.isFirst(),
                pageResult.isLast()
        );
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<MachineryResponseDTO> queryMachinery(MachineryFilterDTO filter, UserPrincipal currentUser) {
        return getMachineryPaged(
                filter.getCompanyId(),
                filter.getPage(),
                filter.getSize(),
                filter.getSearch(),
                filter.getStatus(),
                filter.getSort(),
                currentUser
        );
    }

    @Transactional(readOnly = true)
    public MachineryResponseDTO getMachineryById(UUID id, UserPrincipal currentUser) {
        MachineryEquipmentEntity machinery = machineryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipo no encontrado con id: " + id));

        validateTenantAccess(machinery.getCompany().getId(), currentUser);
        return mapToDTO(machinery);
    }

    @Transactional
    public MachineryResponseDTO updateMachinery(UUID id, MachineryUpdateDTO dto, UserPrincipal currentUser) {
        MachineryEquipmentEntity machinery = machineryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipo no encontrado con id: " + id));

        validateTenantAccess(machinery.getCompany().getId(), currentUser);

        if (!dto.getCode().equals(machinery.getCode())) {
            if (machineryRepository.existsByCompanyIdAndCode(machinery.getCompany().getId(), dto.getCode())) {
                throw new DuplicateResourceException("El código de equipo '" + dto.getCode() + "' ya está registrado en esta empresa.");
            }
            machinery.setCode(dto.getCode());
        }

        machinery.setName(dto.getName());
        if (dto.getStatus() != null && !dto.getStatus().trim().isEmpty()) {
            machinery.setStatus(dto.getStatus().trim().toUpperCase());
        }
        machinery.setLastMaintenanceDate(dto.getLastMaintenanceDate());
        machinery.setNextMaintenanceDate(dto.getNextMaintenanceDate());

        MachineryEquipmentEntity updated = machineryRepository.save(machinery);

        UUID actorId = currentUser != null ? currentUser.getId() : null;
        auditLogService.log(
                machinery.getCompany().getId(),
                actorId,
                "UPDATE",
                "MACHINERY",
                "Equipo actualizado: [" + updated.getCode() + "]"
        );

        return mapToDTO(updated);
    }

    @Transactional
    public MachineryResponseDTO updateStatus(UUID id, MachineryStatusUpdateDTO dto, UserPrincipal currentUser) {
        MachineryEquipmentEntity machinery = machineryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipo no encontrado con id: " + id));

        validateTenantAccess(machinery.getCompany().getId(), currentUser);

        machinery.setStatus(dto.getStatus().trim().toUpperCase());
        MachineryEquipmentEntity updated = machineryRepository.save(machinery);

        UUID actorId = currentUser != null ? currentUser.getId() : null;
        auditLogService.log(
                machinery.getCompany().getId(),
                actorId,
                "UPDATE",
                "MACHINERY",
                "Estado de equipo [" + updated.getCode() + "] cambiado a: " + updated.getStatus()
        );

        return mapToDTO(updated);
    }

    @Transactional
    public void deleteMachinery(UUID id, UserPrincipal currentUser) {
        MachineryEquipmentEntity machinery = machineryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipo no encontrado con id: " + id));

        validateTenantAccess(machinery.getCompany().getId(), currentUser);

        machinery.setIsDeleted(true);
        machineryRepository.save(machinery);

        UUID actorId = currentUser != null ? currentUser.getId() : null;
        auditLogService.log(
                machinery.getCompany().getId(),
                actorId,
                "DELETE",
                "MACHINERY",
                "Equipo eliminado lógicamente (id: " + id + ")"
        );
    }

    private void validateTenantAccess(UUID companyId, UserPrincipal currentUser) {
        boolean isSuperAdmin = currentUser != null && "ROLE_SUPERADMIN".equals(currentUser.getRole());
        if (!isSuperAdmin) {
            if (currentUser == null || currentUser.getCompanyId() == null || !currentUser.getCompanyId().equals(companyId)) {
                throw new AccessDeniedException("Acceso denegado: No tiene permisos para acceder a recursos de otra empresa.");
            }
        }
    }

    private Pageable createPageable(int page, int size, String sortStr) {
        int pageNum = Math.max(0, page);
        int pageSize = size > 0 ? size : 10;

        Sort.Direction direction = Sort.Direction.DESC;
        String property = "createdAt";

        if (sortStr != null && !sortStr.trim().isEmpty()) {
            String[] parts = sortStr.split(",");
            String candidateProperty = parts[0].trim();
            if (ALLOWED_SORT_PROPERTIES.contains(candidateProperty)) {
                property = candidateProperty;
            }
            if (parts.length > 1 && parts[1].trim().equalsIgnoreCase("asc")) {
                direction = Sort.Direction.ASC;
            }
        }

        return PageRequest.of(pageNum, pageSize, Sort.by(direction, property));
    }

    private MachineryResponseDTO mapToDTO(MachineryEquipmentEntity entity) {
        UUID companyId = entity.getCompany() != null ? entity.getCompany().getId() : null;
        String companyName = entity.getCompany() != null ? entity.getCompany().getBusinessName() : null;

        return new MachineryResponseDTO(
                entity.getId(),
                companyId,
                companyName,
                entity.getCode(),
                entity.getName(),
                entity.getStatus(),
                entity.getLastMaintenanceDate(),
                entity.getNextMaintenanceDate(),
                entity.getCreatedAt()
        );
    }
}
