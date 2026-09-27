package com.sepisac.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sepisac.backend.dto.AuthResponseDTO;
import com.sepisac.backend.dto.AuthResult;
import com.sepisac.backend.dto.LoginRequestDTO;
import com.sepisac.backend.exception.GlobalExceptionHandler;
import com.sepisac.backend.security.AuthCookieProvider;
import com.sepisac.backend.security.UserPrincipal;
import com.sepisac.backend.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Web Layer Unit Tests")
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @Mock
    private AuthCookieProvider authCookieProvider;

    @InjectMocks
    private AuthController authController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginEndpointTests {

        @Test
        @DisplayName("Should return 200 OK with Set-Cookie header and user details without token in body")
        void shouldReturn200WithCookieAndUserDetailsWhenCredentialsAreValid() throws Exception {
            UUID companyId = UUID.randomUUID();
            AuthResponseDTO userDetails = new AuthResponseDTO(
                    "usuario@sepisac.com",
                    "johan_admin",
                    "ROLE_ADMIN_EMPRESA",
                    companyId
            );
            AuthResult authResult = new AuthResult("dummy.jwt.token", userDetails);

            LoginRequestDTO request = new LoginRequestDTO("usuario@sepisac.com", "miPasswordSeguro123");

            ResponseCookie sampleCookie = ResponseCookie.from("jwt_token", "dummy.jwt.token")
                    .httpOnly(true)
                    .secure(false)
                    .path("/")
                    .maxAge(86400)
                    .sameSite("Lax")
                    .build();

            when(authService.login(any(LoginRequestDTO.class))).thenReturn(authResult);
            when(authCookieProvider.createAuthCookie("dummy.jwt.token")).thenReturn(sampleCookie);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("jwt_token=dummy.jwt.token")))
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                    .andExpect(jsonPath("$.email", is("usuario@sepisac.com")))
                    .andExpect(jsonPath("$.username", is("johan_admin")))
                    .andExpect(jsonPath("$.role", is("ROLE_ADMIN_EMPRESA")))
                    .andExpect(jsonPath("$.companyId", is(companyId.toString())))
                    .andExpect(jsonPath("$.token").doesNotExist())
                    .andExpect(jsonPath("$.type").doesNotExist());
        }

        @Test
        @DisplayName("Should return 400 Bad Request with ErrorResponseDTO when email format is invalid")
        void shouldReturn400WhenEmailFormatIsInvalid() throws Exception {
            LoginRequestDTO invalidRequest = new LoginRequestDTO("invalid-email-format", "miPasswordSeguro123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.error", is("Bad Request")))
                    .andExpect(jsonPath("$.message", containsString("email")))
                    .andExpect(jsonPath("$.path", is("/api/auth/login")))
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when required fields are blank")
        void shouldReturn400WhenFieldsAreBlank() throws Exception {
            LoginRequestDTO blankRequest = new LoginRequestDTO("", "");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(blankRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.error", is("Bad Request")))
                    .andExpect(jsonPath("$.path", is("/api/auth/login")))
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("Should return 401 Unauthorized with ErrorResponseDTO when credentials are bad")
        void shouldReturn401WhenCredentialsAreBad() throws Exception {
            LoginRequestDTO request = new LoginRequestDTO("usuario@sepisac.com", "wrongPassword");

            when(authService.login(any(LoginRequestDTO.class)))
                    .thenThrow(new BadCredentialsException("Credenciales inválidas"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status", is(401)))
                    .andExpect(jsonPath("$.error", is("Unauthorized")))
                    .andExpect(jsonPath("$.message", is("Credenciales inválidas")))
                    .andExpect(jsonPath("$.path", is("/api/auth/login")))
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/logout")
    class LogoutEndpointTests {

        @Test
        @DisplayName("Should return 200 OK with clean cookie in Set-Cookie header")
        void shouldReturn200WithCleanCookieOnLogout() throws Exception {
            ResponseCookie cleanCookie = ResponseCookie.from("jwt_token", "")
                    .httpOnly(true)
                    .secure(false)
                    .path("/")
                    .maxAge(0)
                    .sameSite("Lax")
                    .build();

            when(authCookieProvider.createCleanAuthCookie()).thenReturn(cleanCookie);

            mockMvc.perform(post("/api/auth/logout"))
                    .andExpect(status().isOk())
                    .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("jwt_token=")))
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
        }
    }

    @Nested
    @DisplayName("GET /api/auth/me")
    class MeEndpointTests {

        @Test
        @DisplayName("Should return 200 OK with user details when authenticated")
        void shouldReturn200WithUserDetailsWhenAuthenticated() throws Exception {
            UUID userId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UserPrincipal principal = new UserPrincipal(
                    userId,
                    "usuario@sepisac.com",
                    "johan_admin",
                    "password",
                    companyId,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN_EMPRESA")),
                    true
            );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email", is("usuario@sepisac.com")))
                    .andExpect(jsonPath("$.username", is("johan_admin")))
                    .andExpect(jsonPath("$.role", is("ROLE_ADMIN_EMPRESA")))
                    .andExpect(jsonPath("$.companyId", is(companyId.toString())));
        }

        @Test
        @DisplayName("Should return 401 Unauthorized when unauthenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            SecurityContextHolder.clearContext();

            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status", is(401)))
                    .andExpect(jsonPath("$.error", is("Unauthorized")));
        }
    }
}
