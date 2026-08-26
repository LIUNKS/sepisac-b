package com.sepisac.backend.service;

import com.sepisac.backend.dto.CompanyCreateDTO;
import com.sepisac.backend.dto.CompanyFilterDTO;
import com.sepisac.backend.dto.CompanyResponseDTO;
import com.sepisac.backend.dto.PageResponseDTO;
import com.sepisac.backend.exception.DuplicateResourceException;
import com.sepisac.backend.exception.ResourceNotFoundException;
import com.sepisac.backend.model.CompanyEntity;
import com.sepisac.backend.repository.CompanyRepository;
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
public class CompanyService {

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "id", "businessName", "ruc", "subscriptionStatus", "createdAt", "updatedAt"
    );

    private final CompanyRepository companyRepository;
    private final AuditLogService auditLogService;

    public CompanyService(CompanyRepository companyRepository, AuditLogService auditLogService) {
        this.companyRepository = companyRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public CompanyResponseDTO createCompany(CompanyCreateDTO dto, UserPrincipal currentUser) {
        if (companyRepository.existsByRuc(dto.getRuc())) {
            throw new DuplicateResourceException("Ya existe una empresa registrada con el RUC " + dto.getRuc());
        }

        CompanyEntity company = new CompanyEntity();
        company.setBusinessName(dto.getBusinessName());
        company.setRuc(dto.getRuc());
        company.setSubscriptionStatus("ACTIVE");
        company.setIsDeleted(false);

        CompanyEntity savedCompany = companyRepository.save(company);

        UUID currentUserId = currentUser != null ? currentUser.getId() : null;
        auditLogService.log(
                savedCompany.getId(),
                currentUserId,
                "CREATE_COMPANY",
                "COMPANIES",
                "Creación de nueva empresa: " + savedCompany.getBusinessName() + " (RUC: " + savedCompany.getRuc() + ")"
        );

        return mapToDTO(savedCompany);
    }

    @Transactional(readOnly = true)
    public CompanyResponseDTO getCompanyById(UUID id, UserPrincipal currentUser) {
        CompanyEntity company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada con id: " + id));

        if (currentUser != null && !"ROLE_SUPERADMIN".equals(currentUser.getRole())) {
            if (currentUser.getCompanyId() == null || !currentUser.getCompanyId().equals(company.getId())) {
                throw new AccessDeniedException("Acceso denegado: No tiene permisos para consultar información de otra empresa.");
            }
        }

        return mapToDTO(company);
    }

    @Transactional(readOnly = true)
    public List<CompanyResponseDTO> getAllCompanies() {
        return companyRepository.findAll().stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<CompanyResponseDTO> getCompaniesPaged(int page, int size, String search, String subscriptionStatus, String sort) {
        Pageable pageable = createPageable(page, size, sort);
        String searchParam = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String statusParam = (subscriptionStatus != null && !subscriptionStatus.trim().isEmpty()) ? subscriptionStatus.trim() : null;

        Page<CompanyEntity> pageResult = companyRepository.findAllWithFilters(statusParam, searchParam, pageable);
        List<CompanyResponseDTO> content = pageResult.getContent().stream()
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
    public PageResponseDTO<CompanyResponseDTO> queryCompanies(CompanyFilterDTO filter, UserPrincipal currentUser) {
        if (currentUser != null && !"ROLE_SUPERADMIN".equals(currentUser.getRole())) {
            throw new AccessDeniedException("Acceso denegado: Solo SUPERADMIN puede ejecutar búsquedas avanzadas de empresas.");
        }
        return getCompaniesPaged(
                filter.getPage(),
                filter.getSize(),
                filter.getSearch(),
                filter.getSubscriptionStatus(),
                filter.getSort()
        );
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

    private CompanyResponseDTO mapToDTO(CompanyEntity entity) {
        return new CompanyResponseDTO(
                entity.getId(),
                entity.getBusinessName(),
                entity.getRuc(),
                entity.getSubscriptionStatus(),
                entity.getCreatedAt()
        );
    }
}
