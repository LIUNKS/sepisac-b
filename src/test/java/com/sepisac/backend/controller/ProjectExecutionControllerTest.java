package com.sepisac.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sepisac.backend.dto.*;
import com.sepisac.backend.exception.BusinessRuleException;
import com.sepisac.backend.exception.GlobalExceptionHandler;
import com.sepisac.backend.exception.ResourceNotFoundException;
import com.sepisac.backend.service.ProjectExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectExecutionController Web Layer Unit Tests - Sprint 4")
class ProjectExecutionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProjectExecutionService projectExecutionService;

    @InjectMocks
    private ProjectExecutionController projectExecutionController;

    private ObjectMapper objectMapper;
    private UUID projectId;
    private UUID itemId;
    private UUID machineryId;
    private UUID employeeId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(projectExecutionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        projectId = UUID.randomUUID();
        itemId = UUID.randomUUID();
        machineryId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
    }

    @Test
    @DisplayName("POST /api/v1/projects/{id}/inventory-consumptions - Éxito 201")
    void shouldRegisterInventoryConsumptionSuccessfully() throws Exception {
        ProjectInventoryConsumptionCreateDTO dto = new ProjectInventoryConsumptionCreateDTO(itemId, 5, "Material para tuberías");
        ProjectInventoryConsumptionResponseDTO response = new ProjectInventoryConsumptionResponseDTO(
                UUID.randomUUID(), projectId, itemId, "TUB-01", "Tubo de Acero", 5, 15, OffsetDateTime.now()
        );

        when(projectExecutionService.consumeInventory(eq(projectId), any(ProjectInventoryConsumptionCreateDTO.class), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/projects/{id}/inventory-consumptions", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantityConsumed", is(5)))
                .andExpect(jsonPath("$.remainingStock", is(15)))
                .andExpect(jsonPath("$.itemSku", is("TUB-01")));
    }

    @Test
    @DisplayName("POST /api/v1/projects/{id}/inventory-consumptions - Falla 409 cuando stock es insuficiente")
    void shouldReturnConflictWhenStockInsufficient() throws Exception {
        ProjectInventoryConsumptionCreateDTO dto = new ProjectInventoryConsumptionCreateDTO(itemId, 50, "Exceso");

        when(projectExecutionService.consumeInventory(eq(projectId), any(ProjectInventoryConsumptionCreateDTO.class), any()))
                .thenThrow(new BusinessRuleException("Stock insuficiente para el ítem: Tubo de Acero"));

        mockMvc.perform(post("/api/v1/projects/{id}/inventory-consumptions", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Stock insuficiente para el ítem: Tubo de Acero"));
    }

    @Test
    @DisplayName("POST /api/v1/projects/{id}/machinery-assignments - Éxito 201")
    void shouldAssignMachinerySuccessfully() throws Exception {
        ProjectMachineryAssignmentCreateDTO dto = new ProjectMachineryAssignmentCreateDTO(machineryId, LocalDate.now(), LocalDate.now().plusDays(10));
        ProjectMachineryAssignmentResponseDTO response = new ProjectMachineryAssignmentResponseDTO(
                UUID.randomUUID(), projectId, machineryId, "MAQ-01", "Excavadora CAT", LocalDate.now(), LocalDate.now().plusDays(10), "EN_USO"
        );

        when(projectExecutionService.assignMachinery(eq(projectId), any(ProjectMachineryAssignmentCreateDTO.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/projects/{id}/machinery-assignments", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("EN_USO")))
                .andExpect(jsonPath("$.machineryCode", is("MAQ-01")));
    }

    @Test
    @DisplayName("POST /api/v1/projects/{id}/machinery-assignments - Falla 400 cuando activo no disponible (Test 2)")
    void shouldReturnBadRequestWhenMachineryNotAvailable() throws Exception {
        ProjectMachineryAssignmentCreateDTO dto = new ProjectMachineryAssignmentCreateDTO(machineryId, LocalDate.now(), null);

        when(projectExecutionService.assignMachinery(eq(projectId), any(ProjectMachineryAssignmentCreateDTO.class)))
                .thenThrow(new IllegalArgumentException("La maquinaria 'Excavadora CAT' no está disponible para asignación. Estado actual: EN_MANTENIMIENTO"));

        mockMvc.perform(post("/api/v1/projects/{id}/machinery-assignments", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La maquinaria 'Excavadora CAT' no está disponible para asignación. Estado actual: EN_MANTENIMIENTO"));
    }

    @Test
    @DisplayName("POST /api/v1/projects/{id}/assignments - Éxito 201")
    void shouldAssignEmployeeSuccessfully() throws Exception {
        ProjectAssignmentCreateDTO dto = new ProjectAssignmentCreateDTO(employeeId, "Técnico Electricista", LocalDate.now());
        ProjectAssignmentResponseDTO response = new ProjectAssignmentResponseDTO(
                UUID.randomUUID(), projectId, employeeId, "Juan Pérez", "Electricidad Industrial", "Técnico Electricista", LocalDate.now(), true
        );

        when(projectExecutionService.assignEmployee(eq(projectId), any(ProjectAssignmentCreateDTO.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/projects/{id}/assignments", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeName", is("Juan Pérez")))
                .andExpect(jsonPath("$.assignedRole", is("Técnico Electricista")))
                .andExpect(jsonPath("$.isActive", is(true)));
    }

    @Test
    @DisplayName("GET /api/v1/projects/{id}/inventory-consumptions - Éxito 200")
    void shouldGetInventoryConsumptionsSuccessfully() throws Exception {
        ProjectInventoryConsumptionResponseDTO dto = new ProjectInventoryConsumptionResponseDTO(
                UUID.randomUUID(), projectId, itemId, "TUB-01", "Tubo de Acero", 5, 15, OffsetDateTime.now()
        );

        when(projectExecutionService.getInventoryConsumptions(eq(projectId), any()))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/projects/{id}/inventory-consumptions", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].itemSku", is("TUB-01")))
                .andExpect(jsonPath("$[0].quantityConsumed", is(5)));
    }

    @Test
    @DisplayName("GET /api/v1/projects/{id}/machinery-assignments - Éxito 200")
    void shouldGetMachineryAssignmentsSuccessfully() throws Exception {
        ProjectMachineryAssignmentResponseDTO dto = new ProjectMachineryAssignmentResponseDTO(
                UUID.randomUUID(), projectId, machineryId, "MAQ-01", "Excavadora CAT", LocalDate.now(), null, "EN_USO"
        );

        when(projectExecutionService.getMachineryAssignments(eq(projectId), any()))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/projects/{id}/machinery-assignments", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].machineryCode", is("MAQ-01")))
                .andExpect(jsonPath("$[0].status", is("EN_USO")));
    }

    @Test
    @DisplayName("DELETE /api/v1/projects/{id}/machinery-assignments/{assignmentId} - Éxito 204")
    void shouldReleaseMachineryAssignmentSuccessfully() throws Exception {
        UUID assignmentId = UUID.randomUUID();
        doNothing().when(projectExecutionService).releaseMachineryAssignment(eq(projectId), eq(assignmentId), any());

        mockMvc.perform(delete("/api/v1/projects/{id}/machinery-assignments/{assignmentId}", projectId, assignmentId))
                .andExpect(status().isNoContent());

        verify(projectExecutionService).releaseMachineryAssignment(eq(projectId), eq(assignmentId), any());
    }

    @Test
    @DisplayName("GET /api/v1/projects/{id}/assignments - Éxito 200")
    void shouldGetEmployeeAssignmentsSuccessfully() throws Exception {
        ProjectAssignmentResponseDTO dto = new ProjectAssignmentResponseDTO(
                UUID.randomUUID(), projectId, employeeId, "Juan Pérez", "Electricidad", "Técnico Electricista", LocalDate.now(), true
        );

        when(projectExecutionService.getEmployeeAssignments(eq(projectId), any()))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/projects/{id}/assignments", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].employeeName", is("Juan Pérez")))
                .andExpect(jsonPath("$[0].assignedRole", is("Técnico Electricista")));
    }

    @Test
    @DisplayName("DELETE /api/v1/projects/{id}/assignments/{assignmentId} - Éxito 204")
    void shouldRemoveEmployeeAssignmentSuccessfully() throws Exception {
        UUID assignmentId = UUID.randomUUID();
        doNothing().when(projectExecutionService).removeEmployeeAssignment(eq(projectId), eq(assignmentId), any());

        mockMvc.perform(delete("/api/v1/projects/{id}/assignments/{assignmentId}", projectId, assignmentId))
                .andExpect(status().isNoContent());

        verify(projectExecutionService).removeEmployeeAssignment(eq(projectId), eq(assignmentId), any());
    }
}
