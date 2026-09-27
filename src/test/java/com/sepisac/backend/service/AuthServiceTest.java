package com.sepisac.backend.service;

import com.sepisac.backend.dto.AuthResult;
import com.sepisac.backend.dto.LoginRequestDTO;
import com.sepisac.backend.security.JwtTokenProvider;
import com.sepisac.backend.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private LoginRequestDTO validLoginRequest;
    private UserPrincipal activePrincipal;
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
    }

    @Nested
    @DisplayName("login")
    class LoginTests {

        @Test
        @DisplayName("Should successfully authenticate and return AuthResult with token and user details")
        void shouldAuthenticateSuccessfullyAndReturnAuthResponse() {
            Authentication authentication = mock(Authentication.class);
            when(authentication.getPrincipal()).thenReturn(activePrincipal);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(jwtTokenProvider.generateToken(activePrincipal)).thenReturn("mocked.jwt.token.123");

            AuthResult result = authService.login(validLoginRequest);

            assertThat(result).isNotNull();
            assertThat(result.token()).isEqualTo("mocked.jwt.token.123");
            assertThat(result.responseDTO()).isNotNull();
            assertThat(result.responseDTO().getEmail()).isEqualTo("usuario@sepisac.com");
            assertThat(result.responseDTO().getUsername()).isEqualTo("johan_admin");
            assertThat(result.responseDTO().getRole()).isEqualTo("ROLE_ADMIN_EMPRESA");
            assertThat(result.responseDTO().getCompanyId()).isEqualTo(companyId);

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(jwtTokenProvider).generateToken(activePrincipal);
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
        @DisplayName("Should throw BadCredentialsException when user is not found")
        void shouldThrowBadCredentialsExceptionWhenUserNotFound() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Usuario no encontrado"));

            assertThatThrownBy(() -> authService.login(validLoginRequest))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Usuario no encontrado");
        }
    }
}
