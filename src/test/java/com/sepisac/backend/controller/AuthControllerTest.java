package com.sepisac.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sepisac.backend.dto.AuthResponseDTO;
import com.sepisac.backend.dto.LoginRequestDTO;
import com.sepisac.backend.exception.GlobalExceptionHandler;
import com.sepisac.backend.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Web Layer Unit Tests")
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

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
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginEndpointTests {

        @Test
        @DisplayName("Should return 200 OK with AuthResponseDTO when credentials are valid")
        void shouldReturn200WithAuthResponseWhenCredentialsAreValid() throws Exception {
            UUID companyId = UUID.randomUUID();
            AuthResponseDTO authResponse = new AuthResponseDTO(
                    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.dummyToken",
                    "Bearer",
                    "usuario@sepisac.com",
                    "johan_admin",
                    "ROLE_ADMIN_EMPRESA",
                    companyId
            );

            LoginRequestDTO request = new LoginRequestDTO("usuario@sepisac.com", "miPasswordSeguro123");

            when(authService.login(any(LoginRequestDTO.class))).thenReturn(authResponse);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token", is("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.dummyToken")))
                    .andExpect(jsonPath("$.type", is("Bearer")))
                    .andExpect(jsonPath("$.email", is("usuario@sepisac.com")))
                    .andExpect(jsonPath("$.username", is("johan_admin")))
                    .andExpect(jsonPath("$.role", is("ROLE_ADMIN_EMPRESA")))
                    .andExpect(jsonPath("$.companyId", is(companyId.toString())));
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
}
