package com.sepisac.backend.service;

import com.sepisac.backend.dto.PageResponseDTO;
import com.sepisac.backend.dto.UserCreateDTO;
import com.sepisac.backend.dto.UserFilterDTO;
import com.sepisac.backend.dto.UserResponseDTO;
import com.sepisac.backend.dto.UserUpdateDTO;
import com.sepisac.backend.exception.DuplicateResourceException;
import com.sepisac.backend.exception.ResourceNotFoundException;
import com.sepisac.backend.model.CompanyEntity;
import com.sepisac.backend.model.RoleEntity;
import com.sepisac.backend.model.UserEntity;
import com.sepisac.backend.repository.CompanyRepository;
import com.sepisac.backend.repository.RoleRepository;
import com.sepisac.backend.repository.UserRepository;
import com.sepisac.backend.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class UserService {

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "id", "fullName", "email", "username", "isActive", "createdAt", "updatedAt"
    );

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public UserService(UserRepository userRepository,
                       CompanyRepository companyRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public UserResponseDTO createUser(UserCreateDTO dto, UserPrincipal currentUser) {
        boolean isSuperAdmin = currentUser != null && "ROLE_SUPERADMIN".equals(currentUser.getRole());

        if (!isSuperAdmin && dto.getRoleId() != null && dto.getRoleId() == 1) {
            throw new AccessDeniedException("Acceso denegado: Un Administrador de Empresa no puede asignar el rol SUPERADMIN.");
        }

        UUID targetCompanyId = isSuperAdmin ? dto.getCompanyId() : (currentUser != null ? currentUser.getCompanyId() : dto.getCompanyId());

        String username = (dto.getUsername() != null && !dto.getUsername().trim().isEmpty())
                ? dto.getUsername().trim()
                : extractUsernameFromEmail(dto.getEmail());

        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("Ya existe un usuario registrado con el correo " + dto.getEmail());
        }

        if (targetCompanyId != null) {
            if (userRepository.existsByCompanyIdAndUsername(targetCompanyId, username)) {
                throw new DuplicateResourceException("El nombre de usuario '" + username + "' ya está en uso en esta empresa.");
            }
        } else {
            if (userRepository.existsByUsername(username)) {
                throw new DuplicateResourceException("El nombre de usuario '" + username + "' ya está en uso.");
            }
        }

        RoleEntity role = roleRepository.findById(dto.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con id: " + dto.getRoleId()));

        CompanyEntity company = null;
        if (targetCompanyId != null) {
            company = companyRepository.findById(targetCompanyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada con id: " + targetCompanyId));
        }

        UserEntity user = new UserEntity();
        user.setEmail(dto.getEmail());
        user.setUsername(username);
        user.setFullName(dto.getFullName());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setRole(role);
        user.setCompany(company);
        user.setIsActive(true);
        user.setIsDeleted(false);

        UserEntity savedUser = userRepository.save(user);

        UUID currentUserId = currentUser != null ? currentUser.getId() : null;
        auditLogService.log(
                targetCompanyId,
                currentUserId,
                "CREATE_USER",
                "USERS",
                "Creación de usuario: " + savedUser.getEmail() + " con rol " + role.getName()
        );

        return mapToDTO(savedUser);
    }

    @Transactional(readOnly = true)
    public List<UserResponseDTO> getUsersByCompany(UUID companyId, UserPrincipal currentUser) {
        validateTenantAccess(companyId, currentUser);
        return userRepository.findByCompanyId(companyId).stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<UserResponseDTO> getUsersByCompanyPaged(
            UUID companyId,
            int page,
            int size,
            String search,
            Boolean isActive,
            Integer roleId,
            String sort,
            UserPrincipal currentUser) {

        validateTenantAccess(companyId, currentUser);

        Pageable pageable = createPageable(page, size, sort);
        String searchParam = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        Page<UserEntity> pageResult = userRepository.findByCompanyIdWithFilters(
                companyId,
                isActive,
                roleId,
                searchParam,
                pageable
        );

        List<UserResponseDTO> content = pageResult.getContent().stream()
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
    public PageResponseDTO<UserResponseDTO> queryUsers(UserFilterDTO filter, UserPrincipal currentUser) {
        boolean isSuperAdmin = currentUser != null && "ROLE_SUPERADMIN".equals(currentUser.getRole());
        UUID targetCompanyId = isSuperAdmin ? filter.getCompanyId() : (currentUser != null ? currentUser.getCompanyId() : filter.getCompanyId());

        if (!isSuperAdmin && targetCompanyId == null) {
            throw new AccessDeniedException("Acceso denegado: Debe especificar la empresa a consultar.");
        }

        if (targetCompanyId != null) {
            validateTenantAccess(targetCompanyId, currentUser);
        }

        Pageable pageable = createPageable(filter.getPage(), filter.getSize(), filter.getSort());
        String searchParam = (filter.getSearch() != null && !filter.getSearch().trim().isEmpty()) ? filter.getSearch().trim() : null;

        Page<UserEntity> pageResult;
        if (targetCompanyId != null) {
            pageResult = userRepository.findByCompanyIdWithFilters(
                    targetCompanyId,
                    filter.getIsActive(),
                    filter.getRoleId(),
                    searchParam,
                    pageable
            );
        } else {
            pageResult = userRepository.findAllWithFilters(
                    null,
                    filter.getIsActive(),
                    filter.getRoleId(),
                    searchParam,
                    pageable
            );
        }

        List<UserResponseDTO> content = pageResult.getContent().stream()
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

    @Transactional
    public UserResponseDTO updateUser(UUID id, UserUpdateDTO dto, UserPrincipal currentUser) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));

        boolean isSuperAdmin = currentUser != null && "ROLE_SUPERADMIN".equals(currentUser.getRole());

        if (!isSuperAdmin) {
            if (currentUser == null || currentUser.getCompanyId() == null || user.getCompany() == null
                    || !currentUser.getCompanyId().equals(user.getCompany().getId())) {
                throw new AccessDeniedException("Acceso denegado: No tiene permisos para modificar usuarios de otra empresa.");
            }
            if (dto.getRoleId() != null && dto.getRoleId() == 1) {
                throw new AccessDeniedException("Acceso denegado: Un Administrador de Empresa no puede asignar el rol SUPERADMIN.");
            }
        }

        if (dto.getUsername() != null && !dto.getUsername().trim().isEmpty()) {
            String newUsername = dto.getUsername().trim();
            if (!newUsername.equalsIgnoreCase(user.getUsername())) {
                UUID companyId = user.getCompany() != null ? user.getCompany().getId() : null;
                if (companyId != null && userRepository.existsByCompanyIdAndUsername(companyId, newUsername)) {
                    throw new DuplicateResourceException("El nombre de usuario '" + newUsername + "' ya está en uso en esta empresa.");
                }
                user.setUsername(newUsername);
            }
        }

        user.setFullName(dto.getFullName());

        if (dto.getRoleId() != null && (user.getRole() == null || !dto.getRoleId().equals(user.getRole().getId()))) {
            RoleEntity role = roleRepository.findById(dto.getRoleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con id: " + dto.getRoleId()));
            user.setRole(role);
        }

        UserEntity updatedUser = userRepository.save(user);

        UUID companyId = user.getCompany() != null ? user.getCompany().getId() : null;
        UUID currentUserId = currentUser != null ? currentUser.getId() : null;
        auditLogService.log(
                companyId,
                currentUserId,
                "UPDATE_USER",
                "USERS",
                "Actualización de datos del usuario: " + user.getEmail()
        );

        return mapToDTO(updatedUser);
    }

    @Transactional
    public UserResponseDTO toggleUserStatus(UUID id, UserPrincipal currentUser) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));

        boolean isSuperAdmin = currentUser != null && "ROLE_SUPERADMIN".equals(currentUser.getRole());

        if (!isSuperAdmin) {
            if (currentUser == null || currentUser.getCompanyId() == null || user.getCompany() == null
                    || !currentUser.getCompanyId().equals(user.getCompany().getId())) {
                throw new AccessDeniedException("Acceso denegado: No tiene permisos para modificar usuarios de otra empresa.");
            }
        }

        boolean newStatus = user.getIsActive() == null || !user.getIsActive();
        user.setIsActive(newStatus);

        UserEntity updatedUser = userRepository.save(user);

        UUID companyId = user.getCompany() != null ? user.getCompany().getId() : null;
        UUID currentUserId = currentUser != null ? currentUser.getId() : null;
        auditLogService.log(
                companyId,
                currentUserId,
                "TOGGLE_USER_STATUS",
                "USERS",
                "Cambio de estado activo a " + newStatus + " para el usuario: " + user.getEmail()
        );

        return mapToDTO(updatedUser);
    }

    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(UUID id, UserPrincipal currentUser) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));

        boolean isSuperAdmin = currentUser != null && "ROLE_SUPERADMIN".equals(currentUser.getRole());

        if (!isSuperAdmin) {
            if (currentUser == null || currentUser.getCompanyId() == null || user.getCompany() == null
                    || !currentUser.getCompanyId().equals(user.getCompany().getId())) {
                throw new AccessDeniedException("Acceso denegado: No tiene permisos para consultar información de usuarios de otra empresa.");
            }
        }

        return mapToDTO(user);
    }

    private void validateTenantAccess(UUID companyId, UserPrincipal currentUser) {
        boolean isSuperAdmin = currentUser != null && "ROLE_SUPERADMIN".equals(currentUser.getRole());
        if (!isSuperAdmin) {
            if (currentUser == null || currentUser.getCompanyId() == null || !currentUser.getCompanyId().equals(companyId)) {
                throw new AccessDeniedException("Acceso denegado: No tiene permisos para consultar usuarios de otra empresa.");
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

    private String extractUsernameFromEmail(String email) {
        if (email != null && email.contains("@")) {
            return email.substring(0, email.indexOf('@')).trim();
        }
        return email != null ? email.trim() : "user";
    }

    private UserResponseDTO mapToDTO(UserEntity entity) {
        UUID companyId = entity.getCompany() != null ? entity.getCompany().getId() : null;
        String companyName = entity.getCompany() != null ? entity.getCompany().getBusinessName() : null;
        Integer roleId = entity.getRole() != null ? entity.getRole().getId() : null;
        String roleName = entity.getRole() != null ? entity.getRole().getName() : null;

        return new UserResponseDTO(
                entity.getId(),
                companyId,
                companyName,
                roleId,
                roleName,
                entity.getUsername(),
                entity.getEmail(),
                entity.getFullName(),
                entity.getIsActive(),
                entity.getCreatedAt()
        );
    }
}
