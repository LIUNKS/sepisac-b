package com.sepisac.backend.service;

import com.sepisac.backend.dto.RoleResponseDTO;
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
