package com.sepisac.backend.service;

import com.sepisac.backend.dto.EmployeeCreateDTO;
import com.sepisac.backend.dto.EmployeeFilterDTO;
import com.sepisac.backend.dto.EmployeeResponseDTO;
import com.sepisac.backend.dto.EmployeeUpdateDTO;
import com.sepisac.backend.dto.PageResponseDTO;
import com.sepisac.backend.exception.ResourceNotFoundException;
import com.sepisac.backend.model.CompanyEntity;
import com.sepisac.backend.model.EmployeeEntity;
import com.sepisac.backend.model.UserEntity;
import com.sepisac.backend.repository.CompanyRepository;
import com.sepisac.backend.repository.EmployeeRepository;
import com.sepisac.backend.repository.UserRepository;
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
public class EmployeeService {

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "id", "fullName", "specialty", "contractType", "baseSalary", "currentHourlyCost", "isAvailable", "createdAt", "updatedAt"
    );

    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public EmployeeService(EmployeeRepository employeeRepository,
                           CompanyRepository companyRepository,
                           UserRepository userRepository,
                           AuditLogService auditLogService) {
        this.employeeRepository = employeeRepository;
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public EmployeeResponseDTO createEmployee(EmployeeCreateDTO dto, UserPrincipal currentUser) {
        boolean isSuperAdmin = currentUser != null && "ROLE_SUPERADMIN".equals(currentUser.getRole());
        UUID targetCompanyId = isSuperAdmin ? dto.getCompanyId() : (currentUser != null ? currentUser.getCompanyId() : dto.getCompanyId());

        if (targetCompanyId == null) {
            throw new AccessDeniedException("Acceso denegado: Debe especificar la empresa del empleado.");
        }

        CompanyEntity company = companyRepository.findById(targetCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada con id: " + targetCompanyId));

        UserEntity user = null;
        if (dto.getUserId() != null) {
            user = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + dto.getUserId()));
        }

        EmployeeEntity employee = new EmployeeEntity();
        employee.setCompany(company);
        employee.setUser(user);
        employee.setFullName(dto.getFullName());
        employee.setSpecialty(dto.getSpecialty());
        employee.setContractType(dto.getContractType());
        employee.setBaseSalary(dto.getBaseSalary());
        employee.setCurrentHourlyCost(dto.getCurrentHourlyCost());
        employee.setIsAvailable(true);
        employee.setIsDeleted(false);

        EmployeeEntity saved = employeeRepository.save(employee);

        UUID actorId = currentUser != null ? currentUser.getId() : null;
        auditLogService.log(
                targetCompanyId,
                actorId,
                "CREATE",
                "HR_EMPLOYEES",
                "Empleado creado: " + saved.getFullName() + " (Especialidad: " + saved.getSpecialty() + ")"
        );

        return mapToDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponseDTO> getEmployeesByCompany(UUID companyId, UserPrincipal currentUser) {
        validateTenantAccess(companyId, currentUser);
        return employeeRepository.findByCompanyId(companyId).stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<EmployeeResponseDTO> getEmployeesPaged(
            UUID companyId,
            int page,
            int size,
            String search,
            String contractType,
            Boolean isAvailable,
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
        String contractParam = (contractType != null && !contractType.trim().isEmpty()) ? contractType.trim().toUpperCase() : null;

        Page<EmployeeEntity> pageResult;
        if (targetCompanyId != null) {
            pageResult = employeeRepository.findByCompanyIdWithFilters(
                    targetCompanyId, contractParam, isAvailable, searchParam, pageable
            );
        } else {
            pageResult = employeeRepository.findAllWithFilters(
                    null, contractParam, isAvailable, searchParam, pageable
            );
        }

        List<EmployeeResponseDTO> content = pageResult.getContent().stream()
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
    public PageResponseDTO<EmployeeResponseDTO> queryEmployees(EmployeeFilterDTO filter, UserPrincipal currentUser) {
        return getEmployeesPaged(
                filter.getCompanyId(),
                filter.getPage(),
                filter.getSize(),
                filter.getSearch(),
                filter.getContractType(),
                filter.getIsAvailable(),
                filter.getSort(),
                currentUser
        );
    }

    @Transactional(readOnly = true)
    public EmployeeResponseDTO getEmployeeById(UUID id, UserPrincipal currentUser) {
        EmployeeEntity employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado con id: " + id));

        validateTenantAccess(employee.getCompany().getId(), currentUser);
        return mapToDTO(employee);
    }

    @Transactional
    public EmployeeResponseDTO updateEmployee(UUID id, EmployeeUpdateDTO dto, UserPrincipal currentUser) {
        EmployeeEntity employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado con id: " + id));

        validateTenantAccess(employee.getCompany().getId(), currentUser);

        UserEntity user = null;
        if (dto.getUserId() != null) {
            user = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + dto.getUserId()));
        }

        employee.setUser(user);
        employee.setFullName(dto.getFullName());
        employee.setSpecialty(dto.getSpecialty());
        employee.setContractType(dto.getContractType());
        employee.setBaseSalary(dto.getBaseSalary());
        employee.setCurrentHourlyCost(dto.getCurrentHourlyCost());

        EmployeeEntity updated = employeeRepository.save(employee);

        UUID actorId = currentUser != null ? currentUser.getId() : null;
        auditLogService.log(
                employee.getCompany().getId(),
                actorId,
                "UPDATE",
                "HR_EMPLOYEES",
                "Empleado actualizado: " + updated.getFullName()
        );

        return mapToDTO(updated);
    }

    @Transactional
    public EmployeeResponseDTO toggleAvailability(UUID id, UserPrincipal currentUser) {
        EmployeeEntity employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado con id: " + id));

        validateTenantAccess(employee.getCompany().getId(), currentUser);

        boolean newAvailability = employee.getIsAvailable() == null || !employee.getIsAvailable();
        employee.setIsAvailable(newAvailability);

        EmployeeEntity updated = employeeRepository.save(employee);

        UUID actorId = currentUser != null ? currentUser.getId() : null;
        auditLogService.log(
                employee.getCompany().getId(),
                actorId,
                "UPDATE",
                "HR_EMPLOYEES",
                "Disponibilidad cambiada a " + newAvailability + " para empleado: " + updated.getFullName()
        );

        return mapToDTO(updated);
    }

    @Transactional
    public void deleteEmployee(UUID id, UserPrincipal currentUser) {
        EmployeeEntity employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado con id: " + id));

        validateTenantAccess(employee.getCompany().getId(), currentUser);

        employee.setIsDeleted(true);
        employeeRepository.save(employee);

        UUID actorId = currentUser != null ? currentUser.getId() : null;
        auditLogService.log(
                employee.getCompany().getId(),
                actorId,
                "DELETE",
                "HR_EMPLOYEES",
                "Empleado eliminado lógicamente (id: " + id + ")"
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

    private EmployeeResponseDTO mapToDTO(EmployeeEntity entity) {
        UUID companyId = entity.getCompany() != null ? entity.getCompany().getId() : null;
        String companyName = entity.getCompany() != null ? entity.getCompany().getBusinessName() : null;
        UUID userId = entity.getUser() != null ? entity.getUser().getId() : null;
        String userEmail = entity.getUser() != null ? entity.getUser().getEmail() : null;

        return new EmployeeResponseDTO(
                entity.getId(),
                companyId,
                companyName,
                userId,
                userEmail,
                entity.getFullName(),
                entity.getSpecialty(),
                entity.getContractType(),
                entity.getBaseSalary(),
                entity.getCurrentHourlyCost(),
                entity.getIsAvailable(),
                entity.getCreatedAt()
        );
    }
}
