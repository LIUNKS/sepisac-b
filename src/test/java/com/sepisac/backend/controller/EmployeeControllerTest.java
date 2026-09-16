package com.sepisac.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sepisac.backend.dto.EmployeeCreateDTO;
import com.sepisac.backend.dto.EmployeeResponseDTO;
import com.sepisac.backend.dto.EmployeeUpdateDTO;
import com.sepisac.backend.dto.PageResponseDTO;
import com.sepisac.backend.exception.GlobalExceptionHandler;
import com.sepisac.backend.exception.ResourceNotFoundException;
import com.sepisac.backend.service.EmployeeService;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeController Web Layer Unit Tests")
class EmployeeControllerTest {

    private MockMvc mockMvc;

    @Mock
    private EmployeeService employeeService;

    @InjectMocks
    private EmployeeController employeeController;

    private ObjectMapper objectMapper;
    private UUID testCompanyId;
    private UUID testEmployeeId;
    private EmployeeResponseDTO testEmployeeResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(employeeController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        testCompanyId = UUID.randomUUID();
        testEmployeeId = UUID.randomUUID();
        testEmployeeResponse = new EmployeeResponseDTO(
                testEmployeeId,
                testCompanyId,
                "SEPI S.A.C.",
                null,
                null,
                "Carlos Mendoza",
                "Soldador",
                "PLANILLA",
                new BigDecimal("2800.00"),
                new BigDecimal("35.00"),
                true,
                OffsetDateTime.now()
        );
    }

    @Nested
    @DisplayName("POST /api/employees")
    class CreateEmployeeTests {

        @Test
        @DisplayName("Should return 201 Created when payload is valid")
        void shouldReturn201WhenValid() throws Exception {
            EmployeeCreateDTO request = new EmployeeCreateDTO(
                    testCompanyId, null, "Carlos Mendoza", "Soldador", "PLANILLA",
                    new BigDecimal("2800.00"), new BigDecimal("35.00")
            );

            when(employeeService.createEmployee(any(EmployeeCreateDTO.class), any())).thenReturn(testEmployeeResponse);

            mockMvc.perform(post("/api/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(testEmployeeId.toString())))
                    .andExpect(jsonPath("$.fullName", is("Carlos Mendoza")))
                    .andExpect(jsonPath("$.specialty", is("Soldador")));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when hourly cost is negative")
        void shouldReturn400WhenHourlyCostNegative() throws Exception {
            EmployeeCreateDTO request = new EmployeeCreateDTO(
                    testCompanyId, null, "Carlos Mendoza", "Soldador", "PLANILLA",
                    new BigDecimal("2800.00"), new BigDecimal("-15.00")
            );

            mockMvc.perform(post("/api/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when contract type is invalid")
        void shouldReturn400WhenContractTypeInvalid() throws Exception {
            EmployeeCreateDTO request = new EmployeeCreateDTO(
                    testCompanyId, null, "Carlos Mendoza", "Soldador", "INVENTADO",
                    new BigDecimal("2800.00"), new BigDecimal("35.00")
            );

            mockMvc.perform(post("/api/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));
        }
    }

    @Nested
    @DisplayName("GET /api/employees and GET /api/employees/{id}")
    class GetEmployeesTests {

        @Test
        @DisplayName("Should return 200 OK with employee details")
        void shouldReturn200WithEmployee() throws Exception {
            when(employeeService.getEmployeeById(eq(testEmployeeId), any())).thenReturn(testEmployeeResponse);

            mockMvc.perform(get("/api/employees/{id}", testEmployeeId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(testEmployeeId.toString())))
                    .andExpect(jsonPath("$.fullName", is("Carlos Mendoza")));
        }

        @Test
        @DisplayName("Should return 404 Not Found when employee does not exist")
        void shouldReturn404WhenNotFound() throws Exception {
            when(employeeService.getEmployeeById(eq(testEmployeeId), any()))
                    .thenThrow(new ResourceNotFoundException("Empleado no encontrado"));

            mockMvc.perform(get("/api/employees/{id}", testEmployeeId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)));
        }
    }

    @Nested
    @DisplayName("DELETE /api/employees/{id} (Soft Delete)")
    class DeleteEmployeeTests {

        @Test
        @DisplayName("Should return 204 No Content when soft delete is successful")
        void shouldReturn204WhenDeleted() throws Exception {
            doNothing().when(employeeService).deleteEmployee(eq(testEmployeeId), any());

            mockMvc.perform(delete("/api/employees/{id}", testEmployeeId))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Should return 403 Forbidden when unauthorized")
        void shouldReturn403WhenForbidden() throws Exception {
            doThrow(new AccessDeniedException("Acceso denegado"))
                    .when(employeeService).deleteEmployee(eq(testEmployeeId), any());

            mockMvc.perform(delete("/api/employees/{id}", testEmployeeId))
                    .andExpect(status().isForbidden());
        }
    }
}
