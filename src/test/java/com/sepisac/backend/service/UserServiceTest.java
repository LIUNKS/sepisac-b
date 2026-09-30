package com.sepisac.backend.service;

import com.sepisac.backend.dto.UserCreateDTO;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private UserService userService;

    private UserPrincipal adminEmpresaPrincipal;
    private UserPrincipal superAdminPrincipal;
    private CompanyEntity testCompany;
    private RoleEntity superAdminRole;
    private RoleEntity adminEmpresaRole;
    private RoleEntity gerenciaRole;
    private UserEntity existingUser;

    private UUID companyId;
    private UUID adminEmpresaUserId;
    private UUID superAdminUserId;
    private UUID targetUserId;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        adminEmpresaUserId = UUID.randomUUID();
        superAdminUserId = UUID.randomUUID();
        targetUserId = UUID.randomUUID();

        adminEmpresaPrincipal = new UserPrincipal(
                adminEmpresaUserId,
                "admin@empresa.com",
                "admin.empresa",
                "encodedPassword", "Test User", companyId,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN_EMPRESA")),
                true
        );

        superAdminPrincipal = new UserPrincipal(
                superAdminUserId,
                "superadmin@sepisac.com",
                "superadmin",
                "encodedPassword", "Test User", null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_SUPERADMIN")),
                true
        );

        testCompany = new CompanyEntity();
        testCompany.setId(companyId);
        testCompany.setBusinessName("SEPI S.A.C.");
        testCompany.setRuc("20123456789");
        testCompany.setSubscriptionStatus("ACTIVE");

        superAdminRole = new RoleEntity(1, "SUPERADMIN", "Administrador Global del SaaS");
        adminEmpresaRole = new RoleEntity(2, "ADMIN_EMPRESA", "Administrador de la Empresa");
        gerenciaRole = new RoleEntity(3, "GERENCIA", "Acceso a Reportes y Proyectos");

        existingUser = new UserEntity();
        existingUser.setId(targetUserId);
        existingUser.setCompany(testCompany);
        existingUser.setRole(gerenciaRole);
        existingUser.setUsername("juan.perez");
        existingUser.setEmail("juan.perez@empresa.com");
        existingUser.setPasswordHash("$2a$10$encodedPasswordSample");
        existingUser.setFullName("Juan Pérez");
        existingUser.setIsActive(true);
        existingUser.setIsDeleted(false);
        existingUser.setCreatedAt(OffsetDateTime.now());
    }

    @Nested
    @DisplayName("createUser")
    class CreateUserTests {

        @Test
        @DisplayName("Should create user successfully with explicit username and record audit log")
        void shouldCreateUserSuccessfullyWithExplicitUsername() {
            UserCreateDTO request = new UserCreateDTO(
                    companyId,
                    "carlos.gomez@empresa.com",
                    "carlos.gomez",
                    "Carlos Gómez",
                    "Password123",
                    3
            );

            when(userRepository.existsByEmail("carlos.gomez@empresa.com")).thenReturn(false);
            when(userRepository.existsByCompanyIdAndUsername(companyId, "carlos.gomez")).thenReturn(false);
            when(roleRepository.findById(3)).thenReturn(Optional.of(gerenciaRole));
            when(companyRepository.findById(companyId)).thenReturn(Optional.of(testCompany));
            when(passwordEncoder.encode("Password123")).thenReturn("$2a$10$hashedPassword123");
            when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
                UserEntity u = invocation.getArgument(0);
                u.setId(UUID.randomUUID());
                u.setCreatedAt(OffsetDateTime.now());
                return u;
            });

            UserResponseDTO response = userService.createUser(request, adminEmpresaPrincipal);

            assertThat(response).isNotNull();
            assertThat(response.getEmail()).isEqualTo("carlos.gomez@empresa.com");
            assertThat(response.getUsername()).isEqualTo("carlos.gomez");
            assertThat(response.getFullName()).isEqualTo("Carlos Gómez");
            assertThat(response.getCompanyName()).isEqualTo("SEPI S.A.C.");
            assertThat(response.getRoleName()).isEqualTo("GERENCIA");
            assertThat(response.getIsActive()).isTrue();

            verify(userRepository).save(any(UserEntity.class));
            verify(auditLogService).log(
                    eq(companyId),
                    eq(adminEmpresaUserId),
                    eq("CREATE_USER"),
                    eq("USERS"),
                    any()
            );
        }

        @Test
        @DisplayName("Should autogenerate username from email when username is null or blank")
        void shouldAutogenerateUsernameFromEmailWhenUsernameIsBlank() {
            UserCreateDTO request = new UserCreateDTO(
                    companyId,
                    "marta.lopez@empresa.com",
                    "", // blank username
                    "Marta López",
                    "Password123",
                    3
            );

            when(userRepository.existsByEmail("marta.lopez@empresa.com")).thenReturn(false);
            when(userRepository.existsByCompanyIdAndUsername(companyId, "marta.lopez")).thenReturn(false);
            when(roleRepository.findById(3)).thenReturn(Optional.of(gerenciaRole));
            when(companyRepository.findById(companyId)).thenReturn(Optional.of(testCompany));
            when(passwordEncoder.encode("Password123")).thenReturn("$2a$10$hashedPassword123");
            when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
                UserEntity u = invocation.getArgument(0);
                u.setId(UUID.randomUUID());
                u.setCreatedAt(OffsetDateTime.now());
                return u;
            });

            UserResponseDTO response = userService.createUser(request, adminEmpresaPrincipal);

            assertThat(response).isNotNull();
            assertThat(response.getUsername()).isEqualTo("marta.lopez");
            verify(userRepository).save(any(UserEntity.class));
        }

        @Test
        @DisplayName("Should throw AccessDeniedException when ADMIN_EMPRESA attempts privilege escalation to SUPERADMIN (roleId=1)")
        void shouldThrowAccessDeniedWhenAdminEmpresaAttemptsToAssignSuperAdminRole() {
            UserCreateDTO request = new UserCreateDTO(
                    companyId,
                    "rogue.admin@empresa.com",
                    "rogue.admin",
                    "Rogue Admin",
                    "Password123",
                    1 // SUPERADMIN role
            );

            assertThatThrownBy(() -> userService.createUser(request, adminEmpresaPrincipal))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("SUPERADMIN");

            verify(userRepository, never()).save(any());
            verify(auditLogService, never()).log(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should allow SUPERADMIN to create user with SUPERADMIN role (roleId=1)")
        void shouldAllowSuperAdminToAssignSuperAdminRole() {
            UserCreateDTO request = new UserCreateDTO(
                    null,
                    "new.super@sepisac.com",
                    "new.super",
                    "New Super Admin",
                    "Password123",
                    1
            );

            when(userRepository.existsByEmail("new.super@sepisac.com")).thenReturn(false);
            when(userRepository.existsByUsername("new.super")).thenReturn(false);
            when(roleRepository.findById(1)).thenReturn(Optional.of(superAdminRole));
            when(passwordEncoder.encode("Password123")).thenReturn("$2a$10$hashedPassword123");
            when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
                UserEntity u = invocation.getArgument(0);
                u.setId(UUID.randomUUID());
                u.setCreatedAt(OffsetDateTime.now());
                return u;
            });

            UserResponseDTO response = userService.createUser(request, superAdminPrincipal);

            assertThat(response).isNotNull();
            assertThat(response.getRoleName()).isEqualTo("SUPERADMIN");
            verify(userRepository).save(any(UserEntity.class));
        }

        @Test
        @DisplayName("Should throw DuplicateResourceException when email already exists")
        void shouldThrowDuplicateResourceExceptionWhenEmailExists() {
            UserCreateDTO request = new UserCreateDTO(
                    companyId,
                    "existing@empresa.com",
                    "existing.user",
                    "Existing User",
                    "Password123",
                    3
            );

            when(userRepository.existsByEmail("existing@empresa.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser(request, adminEmpresaPrincipal))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("existing@empresa.com");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw DuplicateResourceException when username already exists in company")
        void shouldThrowDuplicateResourceExceptionWhenUsernameExistsInCompany() {
            UserCreateDTO request = new UserCreateDTO(
                    companyId,
                    "juan.perez2@empresa.com",
                    "juan.perez",
                    "Juan Pérez Jr",
                    "Password123",
                    3
            );

            when(userRepository.existsByEmail("juan.perez2@empresa.com")).thenReturn(false);
            when(userRepository.existsByCompanyIdAndUsername(companyId, "juan.perez")).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser(request, adminEmpresaPrincipal))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("juan.perez");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when role ID does not exist")
        void shouldThrowResourceNotFoundExceptionWhenRoleDoesNotExist() {
            UserCreateDTO request = new UserCreateDTO(
                    companyId,
                    "valid@empresa.com",
                    "valid.user",
                    "Valid User",
                    "Password123",
                    99
            );

            when(userRepository.existsByEmail("valid@empresa.com")).thenReturn(false);
            when(userRepository.existsByCompanyIdAndUsername(companyId, "valid.user")).thenReturn(false);
            when(roleRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.createUser(request, adminEmpresaPrincipal))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getUsersByCompany")
    class GetUsersByCompanyTests {

        @Test
        @DisplayName("Should return users for ADMIN_EMPRESA requesting their own company")
        void shouldReturnUsersForAdminEmpresaOwnCompany() {
            when(userRepository.findByCompanyId(companyId)).thenReturn(List.of(existingUser));

            List<UserResponseDTO> result = userService.getUsersByCompany(companyId, adminEmpresaPrincipal);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUsername()).isEqualTo("juan.perez");
            verify(userRepository).findByCompanyId(companyId);
        }

        @Test
        @DisplayName("Should return users for SUPERADMIN requesting any company")
        void shouldReturnUsersForSuperAdminAnyCompany() {
            when(userRepository.findByCompanyId(companyId)).thenReturn(List.of(existingUser));

            List<UserResponseDTO> result = userService.getUsersByCompany(companyId, superAdminPrincipal);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUsername()).isEqualTo("juan.perez");
            verify(userRepository).findByCompanyId(companyId);
        }

        @Test
        @DisplayName("Should throw AccessDeniedException when ADMIN_EMPRESA requests users from a different company")
        void shouldThrowAccessDeniedWhenAdminEmpresaRequestsDifferentCompanyUsers() {
            UUID otherCompanyId = UUID.randomUUID();

            assertThatThrownBy(() -> userService.getUsersByCompany(otherCompanyId, adminEmpresaPrincipal))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("otra empresa");

            verify(userRepository, never()).findByCompanyId(any());
        }

        @Test
        @DisplayName("Should return paged users with filters for matching company")
        void shouldReturnPagedUsersWithFilters() {
            org.springframework.data.domain.Page<UserEntity> page =
                    new org.springframework.data.domain.PageImpl<>(List.of(existingUser), org.springframework.data.domain.PageRequest.of(0, 10), 1);

            when(userRepository.findByCompanyIdWithFilters(eq(companyId), eq(true), eq(3), eq("juan"), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(page);

            com.sepisac.backend.dto.PageResponseDTO<UserResponseDTO> result =
                    userService.getUsersByCompanyPaged(companyId, 0, 10, "juan", true, 3, "createdAt,desc", adminEmpresaPrincipal);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should query users via HTTP QUERY UserFilterDTO body")
        void shouldQueryUsersViaFilterDto() {
            org.springframework.data.domain.Page<UserEntity> page =
                    new org.springframework.data.domain.PageImpl<>(List.of(existingUser), org.springframework.data.domain.PageRequest.of(0, 10), 1);

            when(userRepository.findByCompanyIdWithFilters(eq(companyId), eq(true), eq(3), eq("juan"), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(page);

            com.sepisac.backend.dto.UserFilterDTO filter =
                    new com.sepisac.backend.dto.UserFilterDTO(companyId, "juan", true, 3, 0, 10, "createdAt,desc");

            com.sepisac.backend.dto.PageResponseDTO<UserResponseDTO> result =
                    userService.queryUsers(filter, adminEmpresaPrincipal);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("updateUser")
    class UpdateUserTests {

        @Test
        @DisplayName("Should update user successfully and record audit log")
        void shouldUpdateUserSuccessfully() {
            UserUpdateDTO request = new UserUpdateDTO("Juan C. Pérez", "jc.perez", 2);

            when(userRepository.findById(targetUserId)).thenReturn(Optional.of(existingUser));
            when(userRepository.existsByCompanyIdAndUsername(companyId, "jc.perez")).thenReturn(false);
            when(roleRepository.findById(2)).thenReturn(Optional.of(adminEmpresaRole));
            when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            UserResponseDTO response = userService.updateUser(targetUserId, request, adminEmpresaPrincipal);

            assertThat(response).isNotNull();
            assertThat(response.getFullName()).isEqualTo("Juan C. Pérez");
            assertThat(response.getUsername()).isEqualTo("jc.perez");
            assertThat(response.getRoleName()).isEqualTo("ADMIN_EMPRESA");

            verify(userRepository).save(any(UserEntity.class));
            verify(auditLogService).log(
                    eq(companyId),
                    eq(adminEmpresaUserId),
                    eq("UPDATE_USER"),
                    eq("USERS"),
                    any()
            );
        }

        @Test
        @DisplayName("Should throw AccessDeniedException when ADMIN_EMPRESA attempts to update user of another company")
        void shouldThrowAccessDeniedWhenAdminEmpresaUpdatesDifferentCompanyUser() {
            UUID otherCompanyId = UUID.randomUUID();
            CompanyEntity otherCompany = new CompanyEntity();
            otherCompany.setId(otherCompanyId);

            UserEntity otherUser = new UserEntity();
            otherUser.setId(UUID.randomUUID());
            otherUser.setCompany(otherCompany);

            when(userRepository.findById(otherUser.getId())).thenReturn(Optional.of(otherUser));

            UserUpdateDTO request = new UserUpdateDTO("Otro Nombre", "otro.user", 2);

            assertThatThrownBy(() -> userService.updateUser(otherUser.getId(), request, adminEmpresaPrincipal))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("otra empresa");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw AccessDeniedException when ADMIN_EMPRESA attempts to escalate user role to SUPERADMIN (roleId=1)")
        void shouldThrowAccessDeniedWhenAdminEmpresaAttemptsToEscalateRoleToSuperAdmin() {
            UserUpdateDTO request = new UserUpdateDTO("Juan Pérez", "juan.perez", 1); // Role 1

            when(userRepository.findById(targetUserId)).thenReturn(Optional.of(existingUser));

            assertThatThrownBy(() -> userService.updateUser(targetUserId, request, adminEmpresaPrincipal))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("SUPERADMIN");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw DuplicateResourceException when new username is already taken in company")
        void shouldThrowDuplicateResourceExceptionWhenUpdatedUsernameExistsInCompany() {
            UserUpdateDTO request = new UserUpdateDTO("Juan Pérez", "taken.username", 3);

            when(userRepository.findById(targetUserId)).thenReturn(Optional.of(existingUser));
            when(userRepository.existsByCompanyIdAndUsername(companyId, "taken.username")).thenReturn(true);

            assertThatThrownBy(() -> userService.updateUser(targetUserId, request, adminEmpresaPrincipal))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("taken.username");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user ID to update does not exist")
        void shouldThrowResourceNotFoundExceptionWhenUserToUpdateNotFound() {
            UUID nonExistentId = UUID.randomUUID();
            UserUpdateDTO request = new UserUpdateDTO("Juan Pérez", "juan.perez", 3);

            when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUser(nonExistentId, request, adminEmpresaPrincipal))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(nonExistentId.toString());

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("toggleUserStatus")
    class ToggleUserStatusTests {

        @Test
        @DisplayName("Should toggle user status from active to inactive and record audit log")
        void shouldToggleStatusFromActiveToInactive() {
            existingUser.setIsActive(true);
            when(userRepository.findById(targetUserId)).thenReturn(Optional.of(existingUser));
            when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            UserResponseDTO response = userService.toggleUserStatus(targetUserId, adminEmpresaPrincipal);

            assertThat(response).isNotNull();
            assertThat(response.getIsActive()).isFalse();

            verify(userRepository).save(existingUser);
            verify(auditLogService).log(
                    eq(companyId),
                    eq(adminEmpresaUserId),
                    eq("TOGGLE_USER_STATUS"),
                    eq("USERS"),
                    any()
            );
        }

        @Test
        @DisplayName("Should toggle user status from inactive to active and record audit log")
        void shouldToggleStatusFromInactiveToActive() {
            existingUser.setIsActive(false);
            when(userRepository.findById(targetUserId)).thenReturn(Optional.of(existingUser));
            when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            UserResponseDTO response = userService.toggleUserStatus(targetUserId, adminEmpresaPrincipal);

            assertThat(response).isNotNull();
            assertThat(response.getIsActive()).isTrue();

            verify(userRepository).save(existingUser);
            verify(auditLogService).log(
                    eq(companyId),
                    eq(adminEmpresaUserId),
                    eq("TOGGLE_USER_STATUS"),
                    eq("USERS"),
                    any()
            );
        }

        @Test
        @DisplayName("Should throw AccessDeniedException when ADMIN_EMPRESA attempts to toggle user of another company")
        void shouldThrowAccessDeniedWhenAdminEmpresaTogglesDifferentCompanyUser() {
            UUID otherCompanyId = UUID.randomUUID();
            CompanyEntity otherCompany = new CompanyEntity();
            otherCompany.setId(otherCompanyId);

            UserEntity otherUser = new UserEntity();
            otherUser.setId(UUID.randomUUID());
            otherUser.setCompany(otherCompany);

            when(userRepository.findById(otherUser.getId())).thenReturn(Optional.of(otherUser));

            assertThatThrownBy(() -> userService.toggleUserStatus(otherUser.getId(), adminEmpresaPrincipal))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("otra empresa");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user ID to toggle does not exist")
        void shouldThrowResourceNotFoundExceptionWhenUserToToggleNotFound() {
            UUID nonExistentId = UUID.randomUUID();
            when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.toggleUserStatus(nonExistentId, adminEmpresaPrincipal))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(nonExistentId.toString());

            verify(userRepository, never()).save(any());
        }
    }
}


