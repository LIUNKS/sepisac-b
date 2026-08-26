package com.sepisac.backend.service;

import com.sepisac.backend.dto.RoleResponseDTO;
import com.sepisac.backend.model.RoleEntity;
import com.sepisac.backend.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleService Unit Tests")
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private RoleService roleService;

    private List<RoleEntity> mockRoles;

    @BeforeEach
    void setUp() {
        mockRoles = List.of(
                new RoleEntity(1, "SUPERADMIN", "Administrador Global del SaaS"),
                new RoleEntity(2, "ADMIN_EMPRESA", "Administrador de la Empresa"),
                new RoleEntity(3, "GERENCIA", "Acceso a Reportes y Proyectos"),
                new RoleEntity(4, "ALMACEN", "Gestión de Inventario"),
                new RoleEntity(5, "TECNICO", "Personal Operativo de Campo")
        );
    }

    @Nested
    @DisplayName("getAllRoles")
    class GetAllRolesTests {

        @Test
        @DisplayName("Should return complete list of roles mapped to RoleResponseDTO")
        void shouldReturnAllRolesMappedToDto() {
            when(roleRepository.findAll()).thenReturn(mockRoles);

            List<RoleResponseDTO> result = roleService.getAllRoles();

            assertThat(result).hasSize(5);
            assertThat(result.get(0).getName()).isEqualTo("SUPERADMIN");
            assertThat(result.get(1).getName()).isEqualTo("ADMIN_EMPRESA");
            assertThat(result.get(2).getName()).isEqualTo("GERENCIA");
            assertThat(result.get(3).getName()).isEqualTo("ALMACEN");
            assertThat(result.get(4).getName()).isEqualTo("TECNICO");

            verify(roleRepository).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no roles are registered")
        void shouldReturnEmptyListWhenNoRoles() {
            when(roleRepository.findAll()).thenReturn(Collections.emptyList());

            List<RoleResponseDTO> result = roleService.getAllRoles();

            assertThat(result).isEmpty();
            verify(roleRepository).findAll();
        }
    }
}
