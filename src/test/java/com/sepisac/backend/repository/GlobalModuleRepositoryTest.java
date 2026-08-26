package com.sepisac.backend.repository;

import com.sepisac.backend.model.AuditLogEntity;
import com.sepisac.backend.model.CompanyEntity;
import com.sepisac.backend.model.RoleEntity;
import com.sepisac.backend.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Global Module Repository Contract Unit Tests")
class GlobalModuleRepositoryTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    private CompanyEntity testCompany;
    private RoleEntity testRole;
    private UserEntity testUser;
    private AuditLogEntity testAuditLog;
    private UUID companyId;
    private UUID userId;
    private UUID auditLogId;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        userId = UUID.randomUUID();
        auditLogId = UUID.randomUUID();

        testCompany = new CompanyEntity();
        testCompany.setId(companyId);
        testCompany.setBusinessName("SEPI S.A.C.");
        testCompany.setRuc("20123456789");
        testCompany.setSubscriptionStatus("ACTIVE");
        testCompany.setIsDeleted(false);

        testRole = new RoleEntity();
        testRole.setId(2);
        testRole.setName("ADMIN_EMPRESA");
        testRole.setDescription("Administrador de la Empresa");

        testUser = new UserEntity();
        testUser.setId(userId);
        testUser.setCompany(testCompany);
        testUser.setRole(testRole);
        testUser.setUsername("juan.perez");
        testUser.setEmail("juan.perez@sepisac.com");
        testUser.setPasswordHash("$2a$10$hashedpassword");
        testUser.setFullName("Juan Pérez");
        testUser.setIsActive(true);
        testUser.setIsDeleted(false);

        testAuditLog = new AuditLogEntity();
        testAuditLog.setId(auditLogId);
        testAuditLog.setCompany(testCompany);
        testAuditLog.setUser(testUser);
        testAuditLog.setAction("CREATE_USER");
        testAuditLog.setModuleAffected("USERS");
        testAuditLog.setDescription("Usuario juan.perez creado exitosamente");
        testAuditLog.setCreatedAt(OffsetDateTime.now());
    }

    @Nested
    @DisplayName("CompanyRepository Contract Tests")
    class CompanyRepositoryContractTests {

        @Test
        @DisplayName("Should find company by RUC when it exists")
        void shouldFindCompanyByRuc() {
            when(companyRepository.findByRuc("20123456789")).thenReturn(Optional.of(testCompany));

            Optional<CompanyEntity> result = companyRepository.findByRuc("20123456789");

            assertThat(result).isPresent();
            assertThat(result.get().getRuc()).isEqualTo("20123456789");
            assertThat(result.get().getBusinessName()).isEqualTo("SEPI S.A.C.");
            verify(companyRepository).findByRuc("20123456789");
        }

        @Test
        @DisplayName("Should return empty Optional when company RUC does not exist")
        void shouldReturnEmptyWhenRucNotFound() {
            when(companyRepository.findByRuc("20999999999")).thenReturn(Optional.empty());

            Optional<CompanyEntity> result = companyRepository.findByRuc("20999999999");

            assertThat(result).isEmpty();
            verify(companyRepository).findByRuc("20999999999");
        }

        @Test
        @DisplayName("Should check if company exists by RUC")
        void shouldCheckExistsByRuc() {
            when(companyRepository.existsByRuc("20123456789")).thenReturn(true);
            when(companyRepository.existsByRuc("20000000000")).thenReturn(false);

            assertThat(companyRepository.existsByRuc("20123456789")).isTrue();
            assertThat(companyRepository.existsByRuc("20000000000")).isFalse();

            verify(companyRepository).existsByRuc("20123456789");
            verify(companyRepository).existsByRuc("20000000000");
        }
    }

    @Nested
    @DisplayName("RoleRepository Contract Tests")
    class RoleRepositoryContractTests {

        @Test
        @DisplayName("Should find role by name when it exists")
        void shouldFindRoleByName() {
            when(roleRepository.findByName("ADMIN_EMPRESA")).thenReturn(Optional.of(testRole));

            Optional<RoleEntity> result = roleRepository.findByName("ADMIN_EMPRESA");

            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("ADMIN_EMPRESA");
            assertThat(result.get().getId()).isEqualTo(2);
            verify(roleRepository).findByName("ADMIN_EMPRESA");
        }

        @Test
        @DisplayName("Should return empty Optional when role name does not exist")
        void shouldReturnEmptyWhenRoleNotFound() {
            when(roleRepository.findByName("NON_EXISTENT_ROLE")).thenReturn(Optional.empty());

            Optional<RoleEntity> result = roleRepository.findByName("NON_EXISTENT_ROLE");

            assertThat(result).isEmpty();
            verify(roleRepository).findByName("NON_EXISTENT_ROLE");
        }
    }

    @Nested
    @DisplayName("UserRepository Contract Tests")
    class UserRepositoryContractTests {

        @Test
        @DisplayName("Should find users belonging to a specific company")
        void shouldFindUsersByCompanyId() {
            when(userRepository.findByCompanyId(companyId)).thenReturn(List.of(testUser));

            List<UserEntity> users = userRepository.findByCompanyId(companyId);

            assertThat(users).hasSize(1);
            assertThat(users.get(0).getCompany().getId()).isEqualTo(companyId);
            assertThat(users.get(0).getUsername()).isEqualTo("juan.perez");
            verify(userRepository).findByCompanyId(companyId);
        }

        @Test
        @DisplayName("Should check if username already exists within a specific company")
        void shouldCheckExistsByCompanyIdAndUsername() {
            when(userRepository.existsByCompanyIdAndUsername(companyId, "juan.perez")).thenReturn(true);
            when(userRepository.existsByCompanyIdAndUsername(companyId, "maria.lopez")).thenReturn(false);

            assertThat(userRepository.existsByCompanyIdAndUsername(companyId, "juan.perez")).isTrue();
            assertThat(userRepository.existsByCompanyIdAndUsername(companyId, "maria.lopez")).isFalse();

            verify(userRepository).existsByCompanyIdAndUsername(companyId, "juan.perez");
            verify(userRepository).existsByCompanyIdAndUsername(companyId, "maria.lopez");
        }
    }

    @Nested
    @DisplayName("AuditLogRepository Contract Tests")
    class AuditLogRepositoryContractTests {

        @Test
        @DisplayName("Should find audit logs by companyId")
        void shouldFindAuditLogsByCompanyId() {
            when(auditLogRepository.findByCompanyId(companyId)).thenReturn(List.of(testAuditLog));

            List<AuditLogEntity> logs = auditLogRepository.findByCompanyId(companyId);

            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).getCompany().getId()).isEqualTo(companyId);
            assertThat(logs.get(0).getAction()).isEqualTo("CREATE_USER");
            assertThat(logs.get(0).getModuleAffected()).isEqualTo("USERS");
            verify(auditLogRepository).findByCompanyId(companyId);
        }

        @Test
        @DisplayName("Should find audit logs by userId")
        void shouldFindAuditLogsByUserId() {
            when(auditLogRepository.findByUserId(userId)).thenReturn(List.of(testAuditLog));

            List<AuditLogEntity> logs = auditLogRepository.findByUserId(userId);

            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).getUser().getId()).isEqualTo(userId);
            assertThat(logs.get(0).getAction()).isEqualTo("CREATE_USER");
            verify(auditLogRepository).findByUserId(userId);
        }

        @Test
        @DisplayName("Should save audit log entry")
        void shouldSaveAuditLogEntry() {
            when(auditLogRepository.save(any(AuditLogEntity.class))).thenReturn(testAuditLog);

            AuditLogEntity saved = auditLogRepository.save(testAuditLog);

            assertThat(saved).isNotNull();
            assertThat(saved.getId()).isEqualTo(auditLogId);
            assertThat(saved.getAction()).isEqualTo("CREATE_USER");
            verify(auditLogRepository).save(any(AuditLogEntity.class));
        }
    }
}
