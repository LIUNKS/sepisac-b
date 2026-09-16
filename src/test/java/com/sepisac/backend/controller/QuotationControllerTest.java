package com.sepisac.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sepisac.backend.dto.*;
import com.sepisac.backend.exception.BusinessRuleException;
import com.sepisac.backend.exception.DuplicateResourceException;
import com.sepisac.backend.exception.GlobalExceptionHandler;
import com.sepisac.backend.exception.ResourceNotFoundException;
import com.sepisac.backend.service.QuotationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuotationController Web Layer Unit Tests")
class QuotationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private QuotationService quotationService;

    @InjectMocks
    private QuotationController quotationController;

    private ObjectMapper objectMapper;
    private UUID testCompanyId;
    private UUID testQuotationId;
    private UUID testDetailId;
    private UUID testLaborId;
    private QuotationFullDetailResponseDTO testFullResponse;
    private QuotationResponseDTO testSummaryResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(quotationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        testCompanyId = UUID.randomUUID();
        testQuotationId = UUID.randomUUID();
        testDetailId = UUID.randomUUID();
        testLaborId = UUID.randomUUID();

        testSummaryResponse = new QuotationResponseDTO(
                testQuotationId, testCompanyId, "SEPI S.A.C.", "COT-2026-001",
                "Minera Antamina S.A.", "Mantenimiento General", "PEN",
                new BigDecimal("1.0000"), new BigDecimal("1000.00"),
                new BigDecimal("20.00"), new BigDecimal("1200.00"),
                "BORRADOR", 1, 1, OffsetDateTime.now()
        );

        testFullResponse = new QuotationFullDetailResponseDTO(
                testQuotationId, testCompanyId, "SEPI S.A.C.", "COT-2026-001",
                "Minera Antamina S.A.", "Mantenimiento General", "PEN",
                new BigDecimal("1.0000"), new BigDecimal("1000.00"),
                new BigDecimal("20.00"), new BigDecimal("1200.00"),
                "BORRADOR", OffsetDateTime.now(),
                Collections.emptyList(), Collections.emptyList()
        );
    }

    @Nested
    @DisplayName("POST /api/quotations")
    class CreateQuotationTests {

        @Test
        @DisplayName("Should return 201 Created when quotation is created successfully")
        void shouldReturn201WhenValid() throws Exception {
            QuotationCreateDTO request = new QuotationCreateDTO(
                    testCompanyId, "COT-2026-001", "Minera Antamina S.A.", "Mantenimiento General",
                    "PEN", new BigDecimal("1.0000"), new BigDecimal("20.00"), null, null
            );

            when(quotationService.createQuotation(any(QuotationCreateDTO.class), any())).thenReturn(testFullResponse);

            mockMvc.perform(post("/api/quotations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(testQuotationId.toString())))
                    .andExpect(jsonPath("$.quotationNumber", is("COT-2026-001")))
                    .andExpect(jsonPath("$.status", is("BORRADOR")));
        }

        @Test
        @DisplayName("Should return 409 Conflict when quotation number already exists")
        void shouldReturn409WhenDuplicate() throws Exception {
            QuotationCreateDTO request = new QuotationCreateDTO(
                    testCompanyId, "COT-2026-001", "Minera Antamina S.A.", "Mantenimiento General",
                    "PEN", new BigDecimal("1.0000"), new BigDecimal("20.00"), null, null
            );

            when(quotationService.createQuotation(any(QuotationCreateDTO.class), any()))
                    .thenThrow(new DuplicateResourceException("El número de cotización 'COT-2026-001' ya existe"));

            mockMvc.perform(post("/api/quotations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)));
        }
    }

    @Nested
    @DisplayName("GET /api/quotations and GET /api/quotations/{id}")
    class ReadQuotationTests {

        @Test
        @DisplayName("Should return 200 OK with full quotation details")
        void shouldReturn200ForGetById() throws Exception {
            when(quotationService.getQuotationById(eq(testQuotationId), any())).thenReturn(testFullResponse);

            mockMvc.perform(get("/api/quotations/{id}", testQuotationId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(testQuotationId.toString())))
                    .andExpect(jsonPath("$.clientName", is("Minera Antamina S.A.")));
        }

        @Test
        @DisplayName("Should return 404 Not Found when quotation does not exist")
        void shouldReturn404WhenNotFound() throws Exception {
            when(quotationService.getQuotationById(eq(testQuotationId), any()))
                    .thenThrow(new ResourceNotFoundException("Cotización no encontrada"));

            mockMvc.perform(get("/api/quotations/{id}", testQuotationId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)));
        }
    }

    @Nested
    @DisplayName("PUT /api/quotations/{id} and PATCH /api/quotations/{id}/status")
    class UpdateAndStatusTests {

        @Test
        @DisplayName("Should return 200 OK when updating quotation header")
        void shouldReturn200WhenUpdated() throws Exception {
            QuotationUpdateDTO updateDTO = new QuotationUpdateDTO(
                    "Minera Renombrada", "Servicio Nuevo", "PEN", new BigDecimal("1.0000"), new BigDecimal("25.00")
            );

            when(quotationService.updateQuotation(eq(testQuotationId), any(QuotationUpdateDTO.class), any()))
                    .thenReturn(testSummaryResponse);

            mockMvc.perform(put("/api/quotations/{id}", testQuotationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(testQuotationId.toString())));
        }

        @Test
        @DisplayName("Should return 409 Conflict when updating status with invalid transition")
        void shouldReturn409OnInvalidTransition() throws Exception {
            QuotationStatusUpdateDTO statusDTO = new QuotationStatusUpdateDTO("APROBADA");

            when(quotationService.updateQuotationStatus(eq(testQuotationId), any(QuotationStatusUpdateDTO.class), any()))
                    .thenThrow(new BusinessRuleException("Transición de estado no permitida: BORRADOR -> APROBADA"));

            mockMvc.perform(patch("/api/quotations/{id}/status", testQuotationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(statusDTO)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)));
        }
    }

    @Nested
    @DisplayName("DELETE /api/quotations/{id}")
    class DeleteQuotationTests {

        @Test
        @DisplayName("Should return 204 No Content when deleting quotation")
        void shouldReturn204WhenDeleted() throws Exception {
            doNothing().when(quotationService).deleteQuotation(eq(testQuotationId), any());

            mockMvc.perform(delete("/api/quotations/{id}", testQuotationId))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Should return 409 Conflict when deleting APROBADA quotation")
        void shouldReturn409WhenDeletingAprobada() throws Exception {
            doThrow(new BusinessRuleException("No se puede eliminar una cotización en estado APROBADA."))
                    .when(quotationService).deleteQuotation(eq(testQuotationId), any());

            mockMvc.perform(delete("/api/quotations/{id}", testQuotationId))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)));
        }
    }

    @Nested
    @DisplayName("Details Sub-Resource Endpoints")
    class DetailEndpointsTests {

        @Test
        @DisplayName("POST /api/quotations/{id}/details should return 201 Created")
        void shouldAddDetail() throws Exception {
            QuotationDetailCreateDTO detailDTO = new QuotationDetailCreateDTO(
                    "Filtro de Aceite", "MATERIAL", 2, new BigDecimal("100.00")
            );
            QuotationDetailResponseDTO detailResponse = new QuotationDetailResponseDTO(
                    testDetailId, testQuotationId, "Filtro de Aceite", "MATERIAL", 2,
                    new BigDecimal("100.00"), new BigDecimal("200.00")
            );

            when(quotationService.addDetail(eq(testQuotationId), any(QuotationDetailCreateDTO.class), any()))
                    .thenReturn(detailResponse);

            mockMvc.perform(post("/api/quotations/{id}/details", testQuotationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(detailDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(testDetailId.toString())))
                    .andExpect(jsonPath("$.subtotal", is(200.00)));
        }

        @Test
        @DisplayName("DELETE /api/quotations/{id}/details/{detailId} should return 204 No Content")
        void shouldDeleteDetail() throws Exception {
            doNothing().when(quotationService).deleteDetail(eq(testQuotationId), eq(testDetailId), any());

            mockMvc.perform(delete("/api/quotations/{id}/details/{detailId}", testQuotationId, testDetailId))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("Labor Requirements Sub-Resource Endpoints")
    class LaborEndpointsTests {

        @Test
        @DisplayName("POST /api/quotations/{id}/labor should return 201 Created")
        void shouldAddLabor() throws Exception {
            QuotationLaborCreateDTO laborDTO = new QuotationLaborCreateDTO(
                    "Técnico Mecánico", 1, 8, new BigDecimal("50.00")
            );
            QuotationLaborResponseDTO laborResponse = new QuotationLaborResponseDTO(
                    testLaborId, testQuotationId, "Técnico Mecánico", 1, 8,
                    new BigDecimal("50.00"), new BigDecimal("400.00")
            );

            when(quotationService.addLaborRequirement(eq(testQuotationId), any(QuotationLaborCreateDTO.class), any()))
                    .thenReturn(laborResponse);

            mockMvc.perform(post("/api/quotations/{id}/labor", testQuotationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(laborDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(testLaborId.toString())))
                    .andExpect(jsonPath("$.subtotalLabor", is(400.00)));
        }

        @Test
        @DisplayName("DELETE /api/quotations/{id}/labor/{laborId} should return 204 No Content")
        void shouldDeleteLabor() throws Exception {
            doNothing().when(quotationService).deleteLaborRequirement(eq(testQuotationId), eq(testLaborId), any());

            mockMvc.perform(delete("/api/quotations/{id}/labor/{laborId}", testQuotationId, testLaborId))
                    .andExpect(status().isNoContent());
        }
    }
}
