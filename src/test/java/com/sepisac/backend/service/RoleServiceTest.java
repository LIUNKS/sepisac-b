package com.sepisac.backend.service;

import com.sepisac.backend.dto.RoleCreateDTO;
import com.sepisac.backend.dto.RoleResponseDTO;
import com.sepisac.backend.exception.DuplicateResourceException;
import com.sepisac.backend.model.RoleEntity;
import com.sepisac.backend.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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

    @Nested
    @DisplayName("createRole")
    class CreateRoleTests {

        @Test
        @DisplayName("Should create role successfully and return RoleResponseDTO")
        void shouldCreateRoleSuccessfully() {
            RoleCreateDTO request = new RoleCreateDTO("AUDITOR", "Auditor contable y operativo");
            RoleEntity savedEntity = new RoleEntity(6, "AUDITOR", "Auditor contable y operativo");

            when(roleRepository.existsByNameIgnoreCase("AUDITOR")).thenReturn(false);
            when(roleRepository.save(any(RoleEntity.class))).thenReturn(savedEntity);

            RoleResponseDTO result = roleService.createRole(request);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(6);
            assertThat(result.getName()).isEqualTo("AUDITOR");
            assertThat(result.getDescription()).isEqualTo("Auditor contable y operativo");

            ArgumentCaptor<RoleEntity> captor = ArgumentCaptor.forClass(RoleEntity.class);
            verify(roleRepository).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("AUDITOR");
            assertThat(captor.getValue().getDescription()).isEqualTo("Auditor contable y operativo");
        }

        @Test
        @DisplayName("Should trim and uppercase role name upon creation")
        void shouldSanitizeRoleName() {
            RoleCreateDTO request = new RoleCreateDTO("  inspector_calidad  ", "Control de calidad");
            RoleEntity savedEntity = new RoleEntity(7, "INSPECTOR_CALIDAD", "Control de calidad");

            when(roleRepository.existsByNameIgnoreCase("INSPECTOR_CALIDAD")).thenReturn(false);
            when(roleRepository.save(any(RoleEntity.class))).thenReturn(savedEntity);

            RoleResponseDTO result = roleService.createRole(request);

            assertThat(result.getName()).isEqualTo("INSPECTOR_CALIDAD");

            ArgumentCaptor<RoleEntity> captor = ArgumentCaptor.forClass(RoleEntity.class);
            verify(roleRepository).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("INSPECTOR_CALIDAD");
        }

        @Test
        @DisplayName("Should throw DuplicateResourceException when role name already exists")
        void shouldThrowExceptionWhenRoleNameExists() {
            RoleCreateDTO request = new RoleCreateDTO("ALMACEN", "Rol repetido");

            when(roleRepository.existsByNameIgnoreCase("ALMACEN")).thenReturn(true);

            assertThatThrownBy(() -> roleService.createRole(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("El rol 'ALMACEN' ya se encuentra registrado");

            verify(roleRepository, never()).save(any());
        }
    }
}
