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
import java.util.Optional;

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

    @Mock
    private UserRepository userRepository;

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

    @Nested
    @DisplayName("getRoleById")
    class GetRoleByIdTests {

        @Test
        @DisplayName("Should return RoleResponseDTO when role is found")
        void shouldReturnRoleWhenFound() {
            RoleEntity role = new RoleEntity(3, "GERENCIA", "Acceso a Reportes y Proyectos");
            when(roleRepository.findById(3)).thenReturn(Optional.of(role));

            RoleResponseDTO result = roleService.getRoleById(3);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(3);
            assertThat(result.getName()).isEqualTo("GERENCIA");
            assertThat(result.getDescription()).isEqualTo("Acceso a Reportes y Proyectos");
            verify(roleRepository).findById(3);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when role does not exist")
        void shouldThrowResourceNotFoundWhenRoleNotFound() {
            when(roleRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roleService.getRoleById(99))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Rol no encontrado con ID: 99");

            verify(roleRepository).findById(99);
        }
    }

    @Nested
    @DisplayName("updateRole")
    class UpdateRoleTests {

        @Test
        @DisplayName("Should update role successfully and return updated RoleResponseDTO")
        void shouldUpdateRoleSuccessfully() {
            RoleEntity existingRole = new RoleEntity(6, "AUDITOR", "Descripción original");
            RoleUpdateDTO updateDTO = new RoleUpdateDTO("AUDITOR_SENIOR", "Descripción actualizada");
            RoleEntity savedEntity = new RoleEntity(6, "AUDITOR_SENIOR", "Descripción actualizada");

            when(roleRepository.findById(6)).thenReturn(Optional.of(existingRole));
            when(roleRepository.existsByNameIgnoreCaseAndIdNot("AUDITOR_SENIOR", 6)).thenReturn(false);
            when(roleRepository.save(any(RoleEntity.class))).thenReturn(savedEntity);

            RoleResponseDTO result = roleService.updateRole(6, updateDTO);

            assertThat(result.getName()).isEqualTo("AUDITOR_SENIOR");
            assertThat(result.getDescription()).isEqualTo("Descripción actualizada");

            ArgumentCaptor<RoleEntity> captor = ArgumentCaptor.forClass(RoleEntity.class);
            verify(roleRepository).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("AUDITOR_SENIOR");
            assertThat(captor.getValue().getDescription()).isEqualTo("Descripción actualizada");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when updating non-existent role")
        void shouldThrowNotFoundWhenUpdatingNonExistentRole() {
            RoleUpdateDTO updateDTO = new RoleUpdateDTO("NUEVO_NOMBRE", "Desc");
            when(roleRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roleService.updateRole(99, updateDTO))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Rol no encontrado con ID: 99");

            verify(roleRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw BusinessRuleException when attempting to rename system role SUPERADMIN")
        void shouldThrowBusinessRuleWhenRenamingSuperadmin() {
            RoleEntity superadminRole = new RoleEntity(1, "SUPERADMIN", "Administrador Global");
            RoleUpdateDTO updateDTO = new RoleUpdateDTO("NUEVO_SUPERADMIN", "Desc");

            when(roleRepository.findById(1)).thenReturn(Optional.of(superadminRole));

            assertThatThrownBy(() -> roleService.updateRole(1, updateDTO))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("No se permite modificar el nombre del rol del sistema 'SUPERADMIN'");

            verify(roleRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should allow updating description of system role SUPERADMIN without changing its name")
        void shouldAllowUpdatingDescriptionOfSuperadmin() {
            RoleEntity superadminRole = new RoleEntity(1, "SUPERADMIN", "Descripción anterior");
            RoleUpdateDTO updateDTO = new RoleUpdateDTO("SUPERADMIN", "Nueva descripción");
            RoleEntity updatedRole = new RoleEntity(1, "SUPERADMIN", "Nueva descripción");

            when(roleRepository.findById(1)).thenReturn(Optional.of(superadminRole));
            when(roleRepository.existsByNameIgnoreCaseAndIdNot("SUPERADMIN", 1)).thenReturn(false);
            when(roleRepository.save(any(RoleEntity.class))).thenReturn(updatedRole);

            RoleResponseDTO result = roleService.updateRole(1, updateDTO);

            assertThat(result.getName()).isEqualTo("SUPERADMIN");
            assertThat(result.getDescription()).isEqualTo("Nueva descripción");
        }

        @Test
        @DisplayName("Should throw DuplicateResourceException when new role name belongs to another role")
        void shouldThrowDuplicateWhenNewNameAlreadyExistsForOtherRole() {
            RoleEntity existingRole = new RoleEntity(6, "AUDITOR", "Desc");
            RoleUpdateDTO updateDTO = new RoleUpdateDTO("ALMACEN", "Desc");

            when(roleRepository.findById(6)).thenReturn(Optional.of(existingRole));
            when(roleRepository.existsByNameIgnoreCaseAndIdNot("ALMACEN", 6)).thenReturn(true);

            assertThatThrownBy(() -> roleService.updateRole(6, updateDTO))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("El rol 'ALMACEN' ya se encuentra registrado");

            verify(roleRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteRole")
    class DeleteRoleTests {

        @Test
        @DisplayName("Should delete role successfully when not a system role and has no users assigned")
        void shouldDeleteRoleSuccessfully() {
            RoleEntity roleToDelete = new RoleEntity(6, "AUDITOR", "Auditor de procesos");

            when(roleRepository.findById(6)).thenReturn(Optional.of(roleToDelete));
            when(userRepository.existsByRoleId(6)).thenReturn(false);

            roleService.deleteRole(6);

            verify(roleRepository).delete(roleToDelete);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when deleting non-existent role")
        void shouldThrowNotFoundWhenDeletingNonExistentRole() {
            when(roleRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roleService.deleteRole(99))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Rol no encontrado con ID: 99");

            verify(roleRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should throw BusinessRuleException when attempting to delete system role")
        void shouldThrowBusinessRuleWhenDeletingSystemRole() {
            RoleEntity superadminRole = new RoleEntity(1, "SUPERADMIN", "Administrador Global");
            when(roleRepository.findById(1)).thenReturn(Optional.of(superadminRole));

            assertThatThrownBy(() -> roleService.deleteRole(1))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("No se permite eliminar un rol del sistema base: SUPERADMIN");

            verify(roleRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should throw BusinessRuleException when role has users assigned")
        void shouldThrowBusinessRuleWhenRoleHasUsersAssigned() {
            RoleEntity roleWithUsers = new RoleEntity(6, "AUDITOR", "Auditor");
            when(roleRepository.findById(6)).thenReturn(Optional.of(roleWithUsers));
            when(userRepository.existsByRoleId(6)).thenReturn(true);

            assertThatThrownBy(() -> roleService.deleteRole(6))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("No se puede eliminar el rol porque tiene usuarios asignados");

            verify(roleRepository, never()).delete(any());
        }
    }
}
