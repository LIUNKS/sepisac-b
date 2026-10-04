package com.sepisac.backend.service;

import com.sepisac.backend.dto.RoleCreateDTO;
import com.sepisac.backend.dto.RoleResponseDTO;
import com.sepisac.backend.dto.RoleUpdateDTO;
import com.sepisac.backend.exception.BusinessRuleException;
import com.sepisac.backend.exception.DuplicateResourceException;
import com.sepisac.backend.exception.ResourceNotFoundException;
import com.sepisac.backend.model.RoleEntity;
import com.sepisac.backend.repository.RoleRepository;
import com.sepisac.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public RoleService(RoleRepository roleRepository, UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public RoleResponseDTO getRoleById(Integer id) {
        RoleEntity role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con ID: " + id));
        return mapToDTO(role);
    }

    @Transactional
    public RoleResponseDTO updateRole(Integer id, RoleUpdateDTO dto) {
        RoleEntity role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con ID: " + id));

        String newName = dto.getName().trim().toUpperCase();

        if ("SUPERADMIN".equalsIgnoreCase(role.getName()) && !role.getName().equalsIgnoreCase(newName)) {
            throw new BusinessRuleException("No se permite modificar el nombre del rol del sistema 'SUPERADMIN'");
        }

        if (roleRepository.existsByNameIgnoreCaseAndIdNot(newName, id)) {
            throw new DuplicateResourceException("El rol '" + newName + "' ya se encuentra registrado");
        }

        role.setName(newName);
        role.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);

        RoleEntity savedRole = roleRepository.save(role);
        return mapToDTO(savedRole);
    }

    @Transactional
    public void deleteRole(Integer id) {
        RoleEntity role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con ID: " + id));

        if ("SUPERADMIN".equalsIgnoreCase(role.getName()) || "ADMIN_EMPRESA".equalsIgnoreCase(role.getName())) {
            throw new BusinessRuleException("No se permite eliminar un rol del sistema base: " + role.getName());
        }

        if (userRepository.existsByRoleId(id)) {
            throw new BusinessRuleException("No se puede eliminar el rol porque tiene usuarios asignados");
        }

        roleRepository.delete(role);
    }

    @Transactional
    public RoleResponseDTO createRole(RoleCreateDTO dto) {
        String roleName = dto.getName().trim().toUpperCase();
        if (roleRepository.existsByNameIgnoreCase(roleName)) {
            throw new DuplicateResourceException("El rol '" + roleName + "' ya se encuentra registrado");
        }

        RoleEntity role = new RoleEntity();
        role.setName(roleName);
        role.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);

        RoleEntity savedRole = roleRepository.save(role);
        return mapToDTO(savedRole);
    }

    @Transactional(readOnly = true)
    public List<RoleResponseDTO> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::mapToDTO)
                .toList();
    }

    private RoleResponseDTO mapToDTO(RoleEntity entity) {
        return new RoleResponseDTO(
                entity.getId(),
                entity.getName(),
                entity.getDescription()
        );
    }
}
