package com.sepisac.backend.controller;

import com.sepisac.backend.dto.RoleResponseDTO;
import com.sepisac.backend.exception.GlobalExceptionHandler;
import com.sepisac.backend.service.RoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleController Web Layer Unit Tests")
class RoleControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RoleService roleService;

    @InjectMocks
    private RoleController roleController;

    private List<RoleResponseDTO> mockRoleDtos;

    @BeforeEach
    void setUp() {
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
}
