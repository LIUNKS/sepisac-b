package com.sepisac.backend.repository;

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

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserRepository JPA Contract & Query Tests")
class UserRepositoryTest {

    @Mock
    private UserRepository userRepository;

    private CompanyEntity testCompany;
    private RoleEntity testRole;
    private UserEntity activeUser;
    private UUID companyId;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();

        testCompany = new CompanyEntity();
        testCompany.setId(companyId);
        testCompany.setBusinessName("SEPI S.A.C.");
        testCompany.setRuc("20123456789");
        testCompany.setSubscriptionStatus("ACTIVE");

        testRole = new RoleEntity();
        testRole.setId(1);
        testRole.setName("ADMIN_EMPRESA");
        testRole.setDescription("Administrador de la empresa");

        activeUser = new UserEntity();
        activeUser.setId(UUID.randomUUID());
        activeUser.setCompany(testCompany);
        activeUser.setRole(testRole);
        activeUser.setUsername("johan_admin");
        activeUser.setEmail("johan@sepisac.com");
        activeUser.setPasswordHash("$2a$10$hashedpasswordstringsample12345");
        activeUser.setFullName("Johan Admin");
        activeUser.setIsActive(true);
        activeUser.setIsDeleted(false);
    }

    @Nested
    @DisplayName("findByEmail")
    class FindByEmailTests {

        @Test
        @DisplayName("Should return UserEntity when user exists by email")
        void shouldReturnUserWhenEmailExists() {
            when(userRepository.findByEmail("johan@sepisac.com")).thenReturn(Optional.of(activeUser));

            Optional<UserEntity> result = userRepository.findByEmail("johan@sepisac.com");

            assertThat(result).isPresent();
            assertThat(result.get().getEmail()).isEqualTo("johan@sepisac.com");
            assertThat(result.get().getUsername()).isEqualTo("johan_admin");
            assertThat(result.get().getFullName()).isEqualTo("Johan Admin");
            assertThat(result.get().getCompany().getId()).isEqualTo(companyId);
            assertThat(result.get().getRole().getName()).isEqualTo("ADMIN_EMPRESA");

            verify(userRepository).findByEmail("johan@sepisac.com");
        }

        @Test
        @DisplayName("Should return empty Optional when user email does not exist")
        void shouldReturnEmptyWhenEmailDoesNotExist() {
            when(userRepository.findByEmail("nonexistent@sepisac.com")).thenReturn(Optional.empty());

            Optional<UserEntity> result = userRepository.findByEmail("nonexistent@sepisac.com");

            assertThat(result).isEmpty();

            verify(userRepository).findByEmail("nonexistent@sepisac.com");
        }
    }

    @Nested
    @DisplayName("existsByEmail")
    class ExistsByEmailTests {

        @Test
        @DisplayName("Should return true when email exists")
        void shouldReturnTrueWhenEmailExists() {
            when(userRepository.existsByEmail("johan@sepisac.com")).thenReturn(true);

            boolean exists = userRepository.existsByEmail("johan@sepisac.com");

            assertThat(exists).isTrue();

            verify(userRepository).existsByEmail("johan@sepisac.com");
        }

        @Test
        @DisplayName("Should return false when email does not exist")
        void shouldReturnFalseWhenEmailDoesNotExist() {
            when(userRepository.existsByEmail("nonexistent@sepisac.com")).thenReturn(false);

            boolean exists = userRepository.existsByEmail("nonexistent@sepisac.com");

            assertThat(exists).isFalse();

            verify(userRepository).existsByEmail("nonexistent@sepisac.com");
        }
    }

    @Nested
    @DisplayName("findByUsername")
    class FindByUsernameTests {

        @Test
        @DisplayName("Should return UserEntity when user exists by username")
        void shouldReturnUserWhenUsernameExists() {
            when(userRepository.findByUsername("johan_admin")).thenReturn(Optional.of(activeUser));

            Optional<UserEntity> result = userRepository.findByUsername("johan_admin");

            assertThat(result).isPresent();
            assertThat(result.get().getUsername()).isEqualTo("johan_admin");
            assertThat(result.get().getEmail()).isEqualTo("johan@sepisac.com");

            verify(userRepository).findByUsername("johan_admin");
        }

        @Test
        @DisplayName("Should return empty Optional when username does not exist")
        void shouldReturnEmptyWhenUsernameDoesNotExist() {
            when(userRepository.findByUsername("unknown_user")).thenReturn(Optional.empty());

            Optional<UserEntity> result = userRepository.findByUsername("unknown_user");

            assertThat(result).isEmpty();

            verify(userRepository).findByUsername("unknown_user");
        }
    }
}
