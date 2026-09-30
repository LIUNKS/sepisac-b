package com.sepisac.backend.service;

import com.sepisac.backend.dto.AuthResult;
import com.sepisac.backend.dto.LoginRequestDTO;
import com.sepisac.backend.dto.Toggle2FaResponseDTO;
import com.sepisac.backend.dto.Verify2FaRequestDTO;
import com.sepisac.backend.exception.BusinessRuleException;
import com.sepisac.backend.model.CompanyEntity;
import com.sepisac.backend.model.RoleEntity;
import com.sepisac.backend.model.UserEntity;
import com.sepisac.backend.repository.UserRepository;
import com.sepisac.backend.security.JwtTokenProvider;
import com.sepisac.backend.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private LoginRequestDTO validLoginRequest;
    private UserPrincipal activePrincipal;
    private UserEntity userEntity;
    private UUID userId;
    private UUID companyId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        companyId = UUID.randomUUID();

        validLoginRequest = new LoginRequestDTO("usuario@sepisac.com", "password123");

        activePrincipal = new UserPrincipal(
                userId,
                "usuario@sepisac.com",
                "johan_admin",
                "encodedPassword",
                companyId,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN_EMPRESA")),
                true
        );

        CompanyEntity company = new CompanyEntity();
        company.setId(companyId);

        RoleEntity role = new RoleEntity();
        role.setName("ROLE_ADMIN_EMPRESA");

        userEntity = new UserEntity();
        userEntity.setId(userId);
        userEntity.setEmail("usuario@sepisac.com");
        userEntity.setUsername("johan_admin");
        userEntity.setPasswordHash("encodedPassword");
        userEntity.setCompany(company);
        userEntity.setRole(role);
        userEntity.setIsActive(true);
        userEntity.setIsDeleted(false);
        userEntity.setTwoFactorEnabled(false);
    }

    @Nested
    @DisplayName("login")
    class LoginTests {

        @Test
        @DisplayName("Should authenticate successfully and return token when 2FA is disabled")
        void shouldAuthenticateSuccessfullyWhen2FaDisabled() {
            Authentication authentication = mock(Authentication.class);
            when(authentication.getPrincipal()).thenReturn(activePrincipal);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
            when(jwtTokenProvider.generateToken(activePrincipal)).thenReturn("mocked.jwt.token.123");

            AuthResult result = authService.login(validLoginRequest);

            assertThat(result).isNotNull();
            assertThat(result.token()).isEqualTo("mocked.jwt.token.123");
            assertThat(result.responseDTO()).isNotNull();
            assertThat(result.responseDTO().getEmail()).isEqualTo("usuario@sepisac.com");
            assertThat(result.responseDTO().getUsername()).isEqualTo("johan_admin");
            assertThat(result.responseDTO().getRole()).isEqualTo("ROLE_ADMIN_EMPRESA");
            assertThat(result.responseDTO().getCompanyId()).isEqualTo(companyId);
            assertThat(result.responseDTO().getTwoFactorRequired()).isFalse();

            verify(jwtTokenProvider).generateToken(activePrincipal);
            verify(emailService, never()).send2FaCode(any(), any());
        }

        @Test
        @DisplayName("Should send 2FA code and return twoFactorRequired=true without token when 2FA is enabled")
        void shouldSend2FaCodeAndRequire2FaWhenEnabled() {
            userEntity.setTwoFactorEnabled(true);

            Authentication authentication = mock(Authentication.class);
            when(authentication.getPrincipal()).thenReturn(activePrincipal);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));

            AuthResult result = authService.login(validLoginRequest);

            assertThat(result).isNotNull();
            assertThat(result.token()).isNull();
            assertThat(result.responseDTO().getTwoFactorRequired()).isTrue();
            assertThat(result.responseDTO().getEmail()).isEqualTo("usuario@sepisac.com");

            ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
            verify(userRepository).save(userCaptor.capture());
            UserEntity savedUser = userCaptor.getValue();
            assertThat(savedUser.getTwoFactorCode()).isNotNull().hasSize(6);
            assertThat(savedUser.getTwoFactorExpiresAt()).isAfter(OffsetDateTime.now());

            verify(emailService).send2FaCode(eq("usuario@sepisac.com"), eq(savedUser.getTwoFactorCode()));
            verify(jwtTokenProvider, never()).generateToken(any());
        }

        @Test
        @DisplayName("Should throw BadCredentialsException when password is invalid")
        void shouldThrowBadCredentialsExceptionWhenPasswordIsInvalid() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Credenciales inválidas"));

            assertThatThrownBy(() -> authService.login(validLoginRequest))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Credenciales inválidas");
        }

        @Test
        @DisplayName("Should throw DisabledException when user account is inactive")
        void shouldThrowDisabledExceptionWhenAccountIsInactive() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new DisabledException("La cuenta de usuario está desactivada"));

            assertThatThrownBy(() -> authService.login(validLoginRequest))
                    .isInstanceOf(DisabledException.class)
                    .hasMessageContaining("desactivada");
        }

        @Test
        @DisplayName("Should throw BadCredentialsException when user is not found in repository")
        void shouldThrowBadCredentialsExceptionWhenUserNotFound() {
            Authentication authentication = mock(Authentication.class);
            when(authentication.getPrincipal()).thenReturn(activePrincipal);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(validLoginRequest))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Usuario no encontrado");
        }
    }

    @Nested
    @DisplayName("verify2Fa")
    class Verify2FaTests {

        @Test
        @DisplayName("Should successfully verify valid 2FA code and return JWT token")
        void shouldVerify2FaSuccessfully() {
            userEntity.setTwoFactorEnabled(true);
            userEntity.setTwoFactorCode("123456");
            userEntity.setTwoFactorExpiresAt(OffsetDateTime.now().plusMinutes(5));

            when(userRepository.findByEmail("usuario@sepisac.com")).thenReturn(Optional.of(userEntity));
            when(jwtTokenProvider.generateToken(any(UserPrincipal.class))).thenReturn("valid.jwt.2fa.token");

            Verify2FaRequestDTO request = new Verify2FaRequestDTO("usuario@sepisac.com", "123456");
            AuthResult result = authService.verify2Fa(request);

            assertThat(result).isNotNull();
            assertThat(result.token()).isEqualTo("valid.jwt.2fa.token");
            assertThat(result.responseDTO().getTwoFactorRequired()).isFalse();
            assertThat(result.responseDTO().getEmail()).isEqualTo("usuario@sepisac.com");

            // Code should be cleaned up
            assertThat(userEntity.getTwoFactorCode()).isNull();
            assertThat(userEntity.getTwoFactorExpiresAt()).isNull();
            verify(userRepository).save(userEntity);
        }

        @Test
        @DisplayName("Should throw BadCredentialsException when 2FA code is incorrect")
        void shouldThrowWhen2FaCodeIsIncorrect() {
            userEntity.setTwoFactorEnabled(true);
            userEntity.setTwoFactorCode("123456");
            userEntity.setTwoFactorExpiresAt(OffsetDateTime.now().plusMinutes(5));

            when(userRepository.findByEmail("usuario@sepisac.com")).thenReturn(Optional.of(userEntity));

            Verify2FaRequestDTO request = new Verify2FaRequestDTO("usuario@sepisac.com", "999999");

            assertThatThrownBy(() -> authService.verify2Fa(request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Código 2FA incorrecto");
        }

        @Test
        @DisplayName("Should throw BadCredentialsException when 2FA code is expired")
        void shouldThrowWhen2FaCodeIsExpired() {
            userEntity.setTwoFactorEnabled(true);
            userEntity.setTwoFactorCode("123456");
            userEntity.setTwoFactorExpiresAt(OffsetDateTime.now().minusMinutes(1));

            when(userRepository.findByEmail("usuario@sepisac.com")).thenReturn(Optional.of(userEntity));

            Verify2FaRequestDTO request = new Verify2FaRequestDTO("usuario@sepisac.com", "123456");

            assertThatThrownBy(() -> authService.verify2Fa(request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("expirado");
        }

        @Test
        @DisplayName("Should throw BusinessRuleException when 2FA is not enabled for user")
        void shouldThrowWhen2FaNotEnabled() {
            userEntity.setTwoFactorEnabled(false);

            when(userRepository.findByEmail("usuario@sepisac.com")).thenReturn(Optional.of(userEntity));

            Verify2FaRequestDTO request = new Verify2FaRequestDTO("usuario@sepisac.com", "123456");

            assertThatThrownBy(() -> authService.verify2Fa(request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("no tiene la autenticación de dos factores habilitada");
        }

        @Test
        @DisplayName("Should throw BadCredentialsException when email does not exist")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findByEmail("inexistente@sepisac.com")).thenReturn(Optional.empty());

            Verify2FaRequestDTO request = new Verify2FaRequestDTO("inexistente@sepisac.com", "123456");

            assertThatThrownBy(() -> authService.verify2Fa(request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Credenciales inválidas");
        }
    }

    @Nested
    @DisplayName("toggle2Fa")
    class Toggle2FaTests {

        @Test
        @DisplayName("Should enable 2FA when currently disabled")
        void shouldEnable2FaWhenDisabled() {
            userEntity.setTwoFactorEnabled(false);
            when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));

            Toggle2FaResponseDTO response = authService.toggle2Fa(userId);

            assertThat(response.twoFactorEnabled()).isTrue();
            assertThat(userEntity.getTwoFactorEnabled()).isTrue();
            verify(userRepository).save(userEntity);
        }

        @Test
        @DisplayName("Should disable 2FA and clear code when currently enabled")
        void shouldDisable2FaWhenEnabled() {
            userEntity.setTwoFactorEnabled(true);
            userEntity.setTwoFactorCode("123456");
            userEntity.setTwoFactorExpiresAt(OffsetDateTime.now().plusMinutes(5));
            when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));

            Toggle2FaResponseDTO response = authService.toggle2Fa(userId);

            assertThat(response.twoFactorEnabled()).isFalse();
            assertThat(userEntity.getTwoFactorEnabled()).isFalse();
            assertThat(userEntity.getTwoFactorCode()).isNull();
            assertThat(userEntity.getTwoFactorExpiresAt()).isNull();
            verify(userRepository).save(userEntity);
        }

        @Test
        @DisplayName("Should throw BadCredentialsException when user not found")
        void shouldThrowWhenUserNotFoundOnToggle() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.toggle2Fa(userId))
                    .isInstanceOf(BadCredentialsException.class);
        }
    }
}
