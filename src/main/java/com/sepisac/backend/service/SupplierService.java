package com.sepisac.backend.service;

import com.sepisac.backend.dto.PageResponseDTO;
import com.sepisac.backend.dto.SupplierCreateDTO;
import com.sepisac.backend.dto.SupplierFilterDTO;
import com.sepisac.backend.dto.SupplierResponseDTO;
import com.sepisac.backend.dto.SupplierUpdateDTO;
import com.sepisac.backend.exception.DuplicateResourceException;
import com.sepisac.backend.exception.ResourceNotFoundException;
import com.sepisac.backend.model.CompanyEntity;
import com.sepisac.backend.model.SupplierEntity;
import com.sepisac.backend.repository.CompanyRepository;
import com.sepisac.backend.repository.SupplierRepository;
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
public class SupplierService {

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "id", "businessName", "ruc", "contactPhone", "email", "createdAt", "updatedAt"
    );

    private final SupplierRepository supplierRepository;
    private final CompanyRepository companyRepository;
    private final AuditLogService auditLogService;

    public SupplierService(SupplierRepository supplierRepository,
                           CompanyRepository companyRepository,
                           AuditLogService auditLogService) {
        this.supplierRepository = supplierRepository;
        this.companyRepository = companyRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public SupplierResponseDTO createSupplier(SupplierCreateDTO dto, UserPrincipal currentUser) {
        boolean isSuperAdmin = currentUser != null && "ROLE_SUPERADMIN".equals(currentUser.getRole());
        UUID targetCompanyId = isSuperAdmin ? dto.getCompanyId() : (currentUser != null ? currentUser.getCompanyId() : dto.getCompanyId());

        if (targetCompanyId == null) {
            throw new AccessDeniedException("Acceso denegado: Debe especificar la empresa del proveedor.");
        }

        if (supplierRepository.existsByCompanyIdAndRuc(targetCompanyId, dto.getRuc())) {
            throw new DuplicateResourceException("Ya existe un proveedor registrado con el RUC " + dto.getRuc() + " en esta empresa.");
        }

        CompanyEntity company = companyRepository.findById(targetCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada con id: " + targetCompanyId));

        SupplierEntity supplier = new SupplierEntity();
        supplier.setCompany(company);
        supplier.setBusinessName(dto.getBusinessName());
        supplier.setRuc(dto.getRuc());
        supplier.setContactPhone(dto.getContactPhone());
        supplier.setEmail(dto.getEmail());
        supplier.setIsDeleted(false);

        SupplierEntity saved = supplierRepository.save(supplier);

        UUID actorId = currentUser != null ? currentUser.getId() : null;
        auditLogService.log(
                targetCompanyId,
                actorId,
                "CREATE",
                "SUPPLIERS",
                "Proveedor registrado: " + saved.getBusinessName() + " (RUC: " + saved.getRuc() + ")"
        );

        return mapToDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<SupplierResponseDTO> getSuppliersByCompany(UUID companyId, UserPrincipal currentUser) {
        validateTenantAccess(companyId, currentUser);
        return supplierRepository.findByCompanyId(companyId).stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<SupplierResponseDTO> getSuppliersPaged(
            UUID companyId,
            int page,
            int size,
            String search,
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

        Page<SupplierEntity> pageResult;
        if (targetCompanyId != null) {
            pageResult = supplierRepository.findByCompanyIdWithFilters(targetCompanyId, searchParam, pageable);
        } else {
            pageResult = supplierRepository.findAllWithFilters(null, searchParam, pageable);
        }

        List<SupplierResponseDTO> content = pageResult.getContent().stream()
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
    public PageResponseDTO<SupplierResponseDTO> querySuppliers(SupplierFilterDTO filter, UserPrincipal currentUser) {
        return getSuppliersPaged(
                filter.getCompanyId(),
                filter.getPage(),
                filter.getSize(),
                filter.getSearch(),
                filter.getSort(),
                currentUser
        );
    }

    @Transactional(readOnly = true)
    public SupplierResponseDTO getSupplierById(UUID id, UserPrincipal currentUser) {
        SupplierEntity supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con id: " + id));

        validateTenantAccess(supplier.getCompany().getId(), currentUser);
        return mapToDTO(supplier);
    }

    @Transactional
    public SupplierResponseDTO updateSupplier(UUID id, SupplierUpdateDTO dto, UserPrincipal currentUser) {
        SupplierEntity supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con id: " + id));

        validateTenantAccess(supplier.getCompany().getId(), currentUser);

        if (!dto.getRuc().equals(supplier.getRuc())) {
            if (supplierRepository.existsByCompanyIdAndRuc(supplier.getCompany().getId(), dto.getRuc())) {
                throw new DuplicateResourceException("Ya existe otro proveedor registrado con el RUC " + dto.getRuc() + " en esta empresa.");
            }
            supplier.setRuc(dto.getRuc());
        }

        supplier.setBusinessName(dto.getBusinessName());
        supplier.setContactPhone(dto.getContactPhone());
        supplier.setEmail(dto.getEmail());

        SupplierEntity updated = supplierRepository.save(supplier);

        UUID actorId = currentUser != null ? currentUser.getId() : null;
        auditLogService.log(
                supplier.getCompany().getId(),
                actorId,
                "UPDATE",
                "SUPPLIERS",
                "Proveedor actualizado: " + updated.getBusinessName()
        );

        return mapToDTO(updated);
    }

    @Transactional
    public void deleteSupplier(UUID id, UserPrincipal currentUser) {
        SupplierEntity supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con id: " + id));

        validateTenantAccess(supplier.getCompany().getId(), currentUser);

        supplier.setIsDeleted(true);
        supplierRepository.save(supplier);

        UUID actorId = currentUser != null ? currentUser.getId() : null;
        auditLogService.log(
                supplier.getCompany().getId(),
                actorId,
                "DELETE",
                "SUPPLIERS",
                "Proveedor eliminado lógicamente (id: " + id + ")"
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

    private SupplierResponseDTO mapToDTO(SupplierEntity entity) {
        UUID companyId = entity.getCompany() != null ? entity.getCompany().getId() : null;
        String companyName = entity.getCompany() != null ? entity.getCompany().getBusinessName() : null;

        return new SupplierResponseDTO(
                entity.getId(),
                companyId,
                companyName,
                entity.getBusinessName(),
                entity.getRuc(),
                entity.getContactPhone(),
                entity.getEmail(),
                entity.getCreatedAt()
        );
    }
}
