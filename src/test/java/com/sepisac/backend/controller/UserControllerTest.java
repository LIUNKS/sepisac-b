package com.sepisac.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sepisac.backend.dto.UserCreateDTO;
import com.sepisac.backend.dto.UserResponseDTO;
import com.sepisac.backend.dto.UserUpdateDTO;
import com.sepisac.backend.exception.DuplicateResourceException;
import com.sepisac.backend.exception.GlobalExceptionHandler;
import com.sepisac.backend.exception.ResourceNotFoundException;
import com.sepisac.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController Web Layer Unit Tests")
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private ObjectMapper objectMapper;
    private UUID testCompanyId;
    private UUID testUserId;
    private UserResponseDTO testUserResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        testCompanyId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        testUserResponse = new UserResponseDTO(
                testUserId,
                testCompanyId,
                "SEPI S.A.C.",
                2,
                "ADMIN_EMPRESA",
                "juan.perez",
                "juan.perez@empresa.com",
                "Juan Pérez",
                true,
                OffsetDateTime.now()
        );
    }

    @Nested
    @DisplayName("POST /api/users")
    class CreateUserEndpointTests {

        @Test
        @DisplayName("Should return 201 Created when request payload is valid")
        void shouldReturn201WhenValid() throws Exception {
            UserCreateDTO request = new UserCreateDTO(
                    testCompanyId,
                    "juan.perez@empresa.com",
                    "juan.perez",
                    "Juan Pérez",
                    "Password123",
                    2
            );

            when(userService.createUser(any(UserCreateDTO.class), any())).thenReturn(testUserResponse);

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(testUserId.toString())))
                    .andExpect(jsonPath("$.email", is("juan.perez@empresa.com")))
                    .andExpect(jsonPath("$.username", is("juan.perez")))
                    .andExpect(jsonPath("$.fullName", is("Juan Pérez")))
                    .andExpect(jsonPath("$.roleName", is("ADMIN_EMPRESA")));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when email format is invalid")
        void shouldReturn400WhenEmailInvalid() throws Exception {
            UserCreateDTO request = new UserCreateDTO(
                    testCompanyId,
                    "invalid-email-format",
                    "juan.perez",
                    "Juan Pérez",
                    "Password123",
                    2
            );

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.message", containsString("email")));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when password has fewer than 6 characters")
        void shouldReturn400WhenPasswordTooShort() throws Exception {
            UserCreateDTO request = new UserCreateDTO(
                    testCompanyId,
                    "juan.perez@empresa.com",
                    "juan.perez",
                    "Juan Pérez",
                    "123", // too short
                    2
            );

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.message", containsString("password")));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when roleId is null")
        void shouldReturn400WhenRoleIdIsNull() throws Exception {
            UserCreateDTO request = new UserCreateDTO(
                    testCompanyId,
                    "juan.perez@empresa.com",
                    "juan.perez",
                    "Juan Pérez",
                    "Password123",
                    null // null roleId
            );

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.message", containsString("roleId")));
        }

        @Test
        @DisplayName("Should return 409 Conflict when email or username is already taken")
        void shouldReturn409WhenDuplicate() throws Exception {
            UserCreateDTO request = new UserCreateDTO(
                    testCompanyId,
                    "juan.perez@empresa.com",
                    "juan.perez",
                    "Juan Pérez",
                    "Password123",
                    2
            );

            when(userService.createUser(any(UserCreateDTO.class), any()))
                    .thenThrow(new DuplicateResourceException("El correo juan.perez@empresa.com ya está registrado"));

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.error", is("Conflict")));
        }

        @Test
        @DisplayName("Should return 403 Forbidden when ADMIN_EMPRESA attempts to assign SUPERADMIN role")
        void shouldReturn403WhenRoleEscalationAttempted() throws Exception {
            UserCreateDTO request = new UserCreateDTO(
                    testCompanyId,
                    "rogue@empresa.com",
                    "rogue",
                    "Rogue User",
                    "Password123",
                    1
            );

            when(userService.createUser(any(UserCreateDTO.class), any()))
                    .thenThrow(new AccessDeniedException("Un Administrador de Empresa no puede asignar el rol SUPERADMIN."));

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status", is(403)))
                    .andExpect(jsonPath("$.error", is("Forbidden")));
        }
    }

    @Nested
    @DisplayName("GET /api/users/company/{companyId} & QUERY /api/users/search")
    class GetUsersByCompanyEndpointTests {

        @Test
        @DisplayName("Should return 200 OK with paged list of users for valid tenant")
        void shouldReturn200WithUsersList() throws Exception {
            com.sepisac.backend.dto.PageResponseDTO<UserResponseDTO> pageResponse =
                    new com.sepisac.backend.dto.PageResponseDTO<>(
                            List.of(testUserResponse), 0, 10, 1, 1, true, true
                    );

            when(userService.getUsersByCompanyPaged(eq(testCompanyId), eq(0), eq(10), any(), any(), any(), any(), any()))
                    .thenReturn(pageResponse);

            mockMvc.perform(get("/api/users/company/{companyId}?page=0&size=10&search=juan", testCompanyId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].id", is(testUserId.toString())))
                    .andExpect(jsonPath("$.content[0].username", is("juan.perez")))
                    .andExpect(jsonPath("$.totalElements", is(1)));
        }

        @Test
        @DisplayName("Should return 200 OK for HTTP QUERY /api/users/search")
        void shouldReturn200ForQueryEndpoint() throws Exception {
            com.sepisac.backend.dto.PageResponseDTO<UserResponseDTO> pageResponse =
                    new com.sepisac.backend.dto.PageResponseDTO<>(
                            List.of(testUserResponse), 0, 10, 1, 1, true, true
                    );

            when(userService.queryUsers(any(com.sepisac.backend.dto.UserFilterDTO.class), any()))
                    .thenReturn(pageResponse);

            com.sepisac.backend.dto.UserFilterDTO filter =
                    new com.sepisac.backend.dto.UserFilterDTO(testCompanyId, "juan", true, 2, 0, 10, "createdAt,desc");

            mockMvc.perform(post("/api/users/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(filter)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.totalElements", is(1)));
        }

        @Test
        @DisplayName("Should return 403 Forbidden when user attempts to list users of another company")
        void shouldReturn403WhenAccessDenied() throws Exception {
            UUID otherCompanyId = UUID.randomUUID();
            when(userService.getUsersByCompanyPaged(eq(otherCompanyId), eq(0), eq(10), any(), any(), any(), any(), any()))
                    .thenThrow(new AccessDeniedException("Acceso denegado"));

            mockMvc.perform(get("/api/users/company/{companyId}", otherCompanyId))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status", is(403)))
                    .andExpect(jsonPath("$.error", is("Forbidden")));
        }
    }

    @Nested
    @DisplayName("PUT /api/users/{id}")
    class UpdateUserEndpointTests {

        @Test
        @DisplayName("Should return 200 OK when user is updated successfully")
        void shouldReturn200WhenUpdateSuccessful() throws Exception {
            UserUpdateDTO request = new UserUpdateDTO("Juan C. Pérez", "jc.perez", 3);

            UserResponseDTO updatedResponse = new UserResponseDTO(
                    testUserId,
                    testCompanyId,
                    "SEPI S.A.C.",
                    3,
                    "GERENCIA",
                    "jc.perez",
                    "juan.perez@empresa.com",
                    "Juan C. Pérez",
                    true,
                    OffsetDateTime.now()
            );

            when(userService.updateUser(eq(testUserId), any(UserUpdateDTO.class), any()))
                    .thenReturn(updatedResponse);

            mockMvc.perform(put("/api/users/{id}", testUserId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fullName", is("Juan C. Pérez")))
                    .andExpect(jsonPath("$.username", is("jc.perez")))
                    .andExpect(jsonPath("$.roleName", is("GERENCIA")));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when fullName is blank")
        void shouldReturn400WhenFullNameBlank() throws Exception {
            UserUpdateDTO request = new UserUpdateDTO("", "jc.perez", 3);

            mockMvc.perform(put("/api/users/{id}", testUserId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.message", containsString("fullName")));
        }

        @Test
        @DisplayName("Should return 404 Not Found when user does not exist")
        void shouldReturn404WhenUserNotFound() throws Exception {
            UUID nonExistentId = UUID.randomUUID();
            UserUpdateDTO request = new UserUpdateDTO("Juan Pérez", "juan.perez", 3);

            when(userService.updateUser(eq(nonExistentId), any(UserUpdateDTO.class), any()))
                    .thenThrow(new ResourceNotFoundException("Usuario no encontrado"));

            mockMvc.perform(put("/api/users/{id}", nonExistentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.error", is("Not Found")));
        }

        @Test
        @DisplayName("Should return 403 Forbidden when ADMIN_EMPRESA attempts to assign SUPERADMIN role on update")
        void shouldReturn403WhenRoleEscalationOnUpdate() throws Exception {
            UserUpdateDTO request = new UserUpdateDTO("Juan Pérez", "juan.perez", 1);

            when(userService.updateUser(eq(testUserId), any(UserUpdateDTO.class), any()))
                    .thenThrow(new AccessDeniedException("Un Administrador de Empresa no puede asignar el rol SUPERADMIN."));

            mockMvc.perform(put("/api/users/{id}", testUserId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status", is(403)))
                    .andExpect(jsonPath("$.error", is("Forbidden")));
        }
    }

    @Nested
    @DisplayName("PATCH /api/users/{id}/toggle-status")
    class ToggleUserStatusEndpointTests {

        @Test
        @DisplayName("Should return 200 OK when user status is toggled successfully")
        void shouldReturn200WhenToggleSuccessful() throws Exception {
            UserResponseDTO toggledResponse = new UserResponseDTO(
                    testUserId,
                    testCompanyId,
                    "SEPI S.A.C.",
                    2,
                    "ADMIN_EMPRESA",
                    "juan.perez",
                    "juan.perez@empresa.com",
                    "Juan Pérez",
                    false, // now inactive
                    OffsetDateTime.now()
            );

            when(userService.toggleUserStatus(eq(testUserId), any())).thenReturn(toggledResponse);

            mockMvc.perform(patch("/api/users/{id}/toggle-status", testUserId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(testUserId.toString())))
                    .andExpect(jsonPath("$.isActive", is(false)));
        }

        @Test
        @DisplayName("Should return 404 Not Found when user to toggle does not exist")
        void shouldReturn404WhenTogglingNonExistentUser() throws Exception {
            UUID nonExistentId = UUID.randomUUID();

            when(userService.toggleUserStatus(eq(nonExistentId), any()))
                    .thenThrow(new ResourceNotFoundException("Usuario no encontrado"));

            mockMvc.perform(patch("/api/users/{id}/toggle-status", nonExistentId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.error", is("Not Found")));
        }

        @Test
        @DisplayName("Should return 403 Forbidden when ADMIN_EMPRESA attempts to toggle user of another company")
        void shouldReturn403WhenTogglingDifferentCompanyUser() throws Exception {
            UUID otherCompanyUserId = UUID.randomUUID();

            when(userService.toggleUserStatus(eq(otherCompanyUserId), any()))
                    .thenThrow(new AccessDeniedException("Acceso denegado"));

            mockMvc.perform(patch("/api/users/{id}/toggle-status", otherCompanyUserId))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status", is(403)))
                    .andExpect(jsonPath("$.error", is("Forbidden")));
        }
    }

    @Nested
    @DisplayName("GET /api/users/{id}")
    class GetUserByIdEndpointTests {

        @Test
        @DisplayName("Should return 200 OK when user is found")
        void shouldReturn200WhenUserFound() throws Exception {
            when(userService.getUserById(eq(testUserId), any())).thenReturn(testUserResponse);

            mockMvc.perform(get("/api/users/{id}", testUserId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(testUserId.toString())))
                    .andExpect(jsonPath("$.username", is("juan.perez")))
                    .andExpect(jsonPath("$.email", is("juan.perez@empresa.com")));
        }

        @Test
        @DisplayName("Should return 404 Not Found when user does not exist")
        void shouldReturn404WhenUserNotFound() throws Exception {
            UUID nonExistentId = UUID.randomUUID();
            when(userService.getUserById(eq(nonExistentId), any()))
                    .thenThrow(new ResourceNotFoundException("Usuario no encontrado"));

            mockMvc.perform(get("/api/users/{id}", nonExistentId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.error", is("Not Found")));
        }

        @Test
        @DisplayName("Should return 403 Forbidden when user belongs to another company")
        void shouldReturn403WhenAccessDenied() throws Exception {
            UUID otherCompanyUserId = UUID.randomUUID();
            when(userService.getUserById(eq(otherCompanyUserId), any()))
                    .thenThrow(new AccessDeniedException("Acceso denegado"));

            mockMvc.perform(get("/api/users/{id}", otherCompanyUserId))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status", is(403)))
                    .andExpect(jsonPath("$.error", is("Forbidden")));
        }
    }
}
