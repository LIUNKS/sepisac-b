package com.sepisac.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sepisac.backend.dto.RoleCreateDTO;
import com.sepisac.backend.dto.RoleResponseDTO;
import com.sepisac.backend.dto.RoleUpdateDTO;
import com.sepisac.backend.exception.BusinessRuleException;
import com.sepisac.backend.exception.DuplicateResourceException;
import com.sepisac.backend.exception.GlobalExceptionHandler;
import com.sepisac.backend.exception.ResourceNotFoundException;
import com.sepisac.backend.service.RoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleController Web Layer Unit Tests")
class RoleControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private RoleService roleService;

    @InjectMocks
    private RoleController roleController;

    private List<RoleResponseDTO> mockRoleDtos;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(roleController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockRoleDtos = List.of(
                new RoleResponseDTO(1, "SUPERADMIN", "Administrador Global del SaaS"),
                new RoleResponseDTO(2, "ADMIN_EMPRESA", "Administrador de la Empresa"),
                new RoleResponseDTO(3, "GERENCIA", "Acceso a Reportes y Proyectos"),
                new RoleResponseDTO(4, "ALMACEN", "Gestión de Inventario"),
                new RoleResponseDTO(5, "TECNICO", "Personal Operativo de Campo")
        );
    }

    @Nested
    @DisplayName("GET /api/roles")
    class GetAllRolesEndpointTests {

        @Test
        @DisplayName("Should return 200 OK with list of all 5 roles in catalogue")
        void shouldReturn200WithRolesList() throws Exception {
            when(roleService.getAllRoles()).thenReturn(mockRoleDtos);

            mockMvc.perform(get("/api/roles"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(5)))
                    .andExpect(jsonPath("$[0].id", is(1)))
                    .andExpect(jsonPath("$[0].name", is("SUPERADMIN")))
                    .andExpect(jsonPath("$[1].id", is(2)))
                    .andExpect(jsonPath("$[1].name", is("ADMIN_EMPRESA")))
                    .andExpect(jsonPath("$[2].id", is(3)))
                    .andExpect(jsonPath("$[2].name", is("GERENCIA")))
                    .andExpect(jsonPath("$[3].id", is(4)))
                    .andExpect(jsonPath("$[3].name", is("ALMACEN")))
                    .andExpect(jsonPath("$[4].id", is(5)))
                    .andExpect(jsonPath("$[4].name", is("TECNICO")));

            verify(roleService).getAllRoles();
        }
    }

    @Nested
    @DisplayName("POST /api/roles")
    class CreateRoleEndpointTests {

        @Test
        @DisplayName("Should return 201 Created with RoleResponseDTO when role is valid")
        void shouldReturn201WhenRoleCreatedSuccessfully() throws Exception {
            RoleCreateDTO request = new RoleCreateDTO("AUDITOR", "Auditor contable y operativo");
            RoleResponseDTO response = new RoleResponseDTO(6, "AUDITOR", "Auditor contable y operativo");

            when(roleService.createRole(any(RoleCreateDTO.class))).thenReturn(response);

            mockMvc.perform(post("/api/roles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(6)))
                    .andExpect(jsonPath("$.name", is("AUDITOR")))
                    .andExpect(jsonPath("$.description", is("Auditor contable y operativo")));

            verify(roleService).createRole(any(RoleCreateDTO.class));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when role name is blank")
        void shouldReturn400WhenNameIsBlank() throws Exception {
            RoleCreateDTO invalidRequest = new RoleCreateDTO("", "Descripción");

            mockMvc.perform(post("/api/roles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("Should return 409 Conflict when role name already exists")
        void shouldReturn409WhenRoleAlreadyExists() throws Exception {
            RoleCreateDTO request = new RoleCreateDTO("ALMACEN", "Rol repetido");

            when(roleService.createRole(any(RoleCreateDTO.class)))
                    .thenThrow(new DuplicateResourceException("El rol 'ALMACEN' ya se encuentra registrado"));

            mockMvc.perform(post("/api/roles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message", is("El rol 'ALMACEN' ya se encuentra registrado")));
        }
    }

    @Nested
    @DisplayName("GET /api/roles/{id}")
    class GetRoleByIdEndpointTests {

        @Test
        @DisplayName("Should return 200 OK with RoleResponseDTO when role exists")
        void shouldReturn200WhenRoleExists() throws Exception {
            RoleResponseDTO roleResponse = new RoleResponseDTO(3, "GERENCIA", "Acceso a Reportes y Proyectos");
            when(roleService.getRoleById(3)).thenReturn(roleResponse);

            mockMvc.perform(get("/api/roles/{id}", 3))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(3)))
                    .andExpect(jsonPath("$.name", is("GERENCIA")))
                    .andExpect(jsonPath("$.description", is("Acceso a Reportes y Proyectos")));

            verify(roleService).getRoleById(3);
        }

        @Test
        @DisplayName("Should return 404 Not Found when role does not exist")
        void shouldReturn404WhenRoleNotFound() throws Exception {
            when(roleService.getRoleById(99))
                    .thenThrow(new ResourceNotFoundException("Rol no encontrado con ID: 99"));

            mockMvc.perform(get("/api/roles/{id}", 99))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message", is("Rol no encontrado con ID: 99")));

            verify(roleService).getRoleById(99);
        }
    }

    @Nested
    @DisplayName("PUT /api/roles/{id}")
    class UpdateRoleEndpointTests {

        @Test
        @DisplayName("Should return 200 OK with updated RoleResponseDTO when request is valid")
        void shouldReturn200WhenUpdateIsSuccessful() throws Exception {
            RoleUpdateDTO request = new RoleUpdateDTO("AUDITOR_SENIOR", "Descripción actualizada");
            RoleResponseDTO response = new RoleResponseDTO(6, "AUDITOR_SENIOR", "Descripción actualizada");

            when(roleService.updateRole(eq(6), any(RoleUpdateDTO.class))).thenReturn(response);

            mockMvc.perform(put("/api/roles/{id}", 6)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(6)))
                    .andExpect(jsonPath("$.name", is("AUDITOR_SENIOR")))
                    .andExpect(jsonPath("$.description", is("Descripción actualizada")));

            verify(roleService).updateRole(eq(6), any(RoleUpdateDTO.class));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when update name is blank")
        void shouldReturn400WhenUpdateNameIsBlank() throws Exception {
            RoleUpdateDTO invalidRequest = new RoleUpdateDTO("", "Descripción");

            mockMvc.perform(put("/api/roles/{id}", 6)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("Should return 404 Not Found when updating non-existent role")
        void shouldReturn404WhenUpdatingNonExistentRole() throws Exception {
            RoleUpdateDTO request = new RoleUpdateDTO("NUEVO_ROL", "Desc");

            when(roleService.updateRole(eq(99), any(RoleUpdateDTO.class)))
                    .thenThrow(new ResourceNotFoundException("Rol no encontrado con ID: 99"));

            mockMvc.perform(put("/api/roles/{id}", 99)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message", is("Rol no encontrado con ID: 99")));
        }

        @Test
        @DisplayName("Should return 409 Conflict when update triggers duplicate or business rule exception")
        void shouldReturn409WhenBusinessRuleOrDuplicateConflict() throws Exception {
            RoleUpdateDTO request = new RoleUpdateDTO("NUEVO_SUPERADMIN", "Desc");

            when(roleService.updateRole(eq(1), any(RoleUpdateDTO.class)))
                    .thenThrow(new BusinessRuleException("No se permite modificar el nombre del rol del sistema 'SUPERADMIN'"));

            mockMvc.perform(put("/api/roles/{id}", 1)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message", is("No se permite modificar el nombre del rol del sistema 'SUPERADMIN'")));
        }
    }

    @Nested
    @DisplayName("DELETE /api/roles/{id}")
    class DeleteRoleEndpointTests {

        @Test
        @DisplayName("Should return 204 No Content when role is deleted successfully")
        void shouldReturn204WhenDeletedSuccessfully() throws Exception {
            doNothing().when(roleService).deleteRole(6);

            mockMvc.perform(delete("/api/roles/{id}", 6))
                    .andExpect(status().isNoContent());

            verify(roleService).deleteRole(6);
        }

        @Test
        @DisplayName("Should return 404 Not Found when deleting non-existent role")
        void shouldReturn404WhenDeletingNonExistentRole() throws Exception {
            doThrow(new ResourceNotFoundException("Rol no encontrado con ID: 99"))
                    .when(roleService).deleteRole(99);

            mockMvc.perform(delete("/api/roles/{id}", 99))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message", is("Rol no encontrado con ID: 99")));
        }

        @Test
        @DisplayName("Should return 409 Conflict when deleting system role or role with assigned users")
        void shouldReturn409WhenDeletingRoleFailsBusinessRule() throws Exception {
            doThrow(new BusinessRuleException("No se permite eliminar un rol del sistema base: SUPERADMIN"))
                    .when(roleService).deleteRole(1);

            mockMvc.perform(delete("/api/roles/{id}", 1))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message", is("No se permite eliminar un rol del sistema base: SUPERADMIN")));
        }
    }
}
