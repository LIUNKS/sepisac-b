package com.sepisac.backend.service;

import com.sepisac.backend.dto.RoleCreateDTO;
import com.sepisac.backend.dto.RoleResponseDTO;
import com.sepisac.backend.exception.DuplicateResourceException;
import com.sepisac.backend.model.RoleEntity;
import com.sepisac.backend.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
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
