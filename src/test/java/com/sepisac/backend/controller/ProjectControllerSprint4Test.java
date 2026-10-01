package com.sepisac.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sepisac.backend.dto.ProjectResponseDTO;
import com.sepisac.backend.dto.ProjectStatusUpdateDTO;
import com.sepisac.backend.exception.BusinessRuleException;
import com.sepisac.backend.exception.GlobalExceptionHandler;
import com.sepisac.backend.exception.ResourceNotFoundException;
import com.sepisac.backend.service.ProjectExecutionService;
import com.sepisac.backend.service.ProjectSeedService;
import com.sepisac.backend.service.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectController Sprint 4 Endpoints Unit Tests")
class ProjectControllerSprint4Test {

    private MockMvc mockMvc;

    @Mock
    private ProjectService projectService;

    @Mock
    private ProjectSeedService projectSeedService;

    @Mock
    private ProjectExecutionService projectExecutionService;

    @InjectMocks
    private ProjectController projectController;

    private ObjectMapper objectMapper;
    private UUID quotationId;
    private UUID projectId;
    private ProjectResponseDTO projectResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(projectController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        quotationId = UUID.randomUUID();
        projectId = UUID.randomUUID();

        projectResponse = new ProjectResponseDTO();
        projectResponse.setId(projectId);
        projectResponse.setCode("PRJ-COT-2026-001");
        projectResponse.setTitle("Instalación de Tuberías");
        projectResponse.setClientName("Minera Antamina S.A.");
        projectResponse.setStatus("PENDIENTE");
        projectResponse.setStartDate(LocalDate.now());
        projectResponse.setCreatedAt(OffsetDateTime.now());
    }

    @Test
    @DisplayName("POST /api/v1/projects/from-quotation/{quotationId} - Éxito 201 Retorna ID del Proyecto")
    void shouldCreateProjectFromQuotationSuccessfully() throws Exception {
        when(projectExecutionService.createProjectFromQuotation(quotationId)).thenReturn(projectResponse);

        mockMvc.perform(post("/api/v1/projects/from-quotation/{quotationId}", quotationId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(projectId.toString())))
                .andExpect(jsonPath("$.code", is("PRJ-COT-2026-001")))
                .andExpect(jsonPath("$.status", is("PENDIENTE")));
    }

    @Test
    @DisplayName("POST /api/v1/projects/from-quotation/{quotationId} - Falla 409 cuando cotización no está aprobada")
    void shouldReturnConflictWhenQuotationNotApproved() throws Exception {
        when(projectExecutionService.createProjectFromQuotation(quotationId))
                .thenThrow(new BusinessRuleException("Solo se pueden crear proyectos a partir de cotizaciones aprobadas"));

        mockMvc.perform(post("/api/v1/projects/from-quotation/{quotationId}", quotationId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Solo se pueden crear proyectos a partir de cotizaciones aprobadas"));
    }

    @Test
    @DisplayName("POST /api/v1/projects/from-quotation/{quotationId} - Falla 404 cuando cotización no existe")
    void shouldReturnNotFoundWhenQuotationDoesNotExist() throws Exception {
        when(projectExecutionService.createProjectFromQuotation(quotationId))
                .thenThrow(new ResourceNotFoundException("Cotización no encontrada"));

        mockMvc.perform(post("/api/v1/projects/from-quotation/{quotationId}", quotationId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Cotización no encontrada"));
    }

    @Test
    @DisplayName("PUT /api/v1/projects/{id}/status - Éxito 200 actualiza estado a EN_PROCESO")
    void shouldUpdateProjectStatusSuccessfully() throws Exception {
        projectResponse.setStatus("EN_PROCESO");
        ProjectStatusUpdateDTO dto = new ProjectStatusUpdateDTO("EN_PROCESO");

        when(projectExecutionService.updateProjectStatus(eq(projectId), any(ProjectStatusUpdateDTO.class)))
                .thenReturn(projectResponse);

        mockMvc.perform(put("/api/v1/projects/{id}/status", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("EN_PROCESO")));
    }

    @Test
    @DisplayName("PUT /api/v1/projects/{id}/status - Falla 400 cuando estado es inválido")
    void shouldReturnBadRequestWhenStatusInvalid() throws Exception {
        ProjectStatusUpdateDTO dto = new ProjectStatusUpdateDTO("INVALIDO");

        mockMvc.perform(put("/api/v1/projects/{id}/status", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }
}
