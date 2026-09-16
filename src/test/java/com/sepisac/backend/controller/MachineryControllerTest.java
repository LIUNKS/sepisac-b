package com.sepisac.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sepisac.backend.dto.MachineryCreateDTO;
import com.sepisac.backend.dto.MachineryResponseDTO;
import com.sepisac.backend.dto.MachineryStatusUpdateDTO;
import com.sepisac.backend.dto.MachineryUpdateDTO;
import com.sepisac.backend.exception.DuplicateResourceException;
import com.sepisac.backend.exception.GlobalExceptionHandler;
import com.sepisac.backend.service.MachineryEquipmentService;
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

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("MachineryController Web Layer Unit Tests")
class MachineryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MachineryEquipmentService machineryService;

    @InjectMocks
    private MachineryController machineryController;

    private ObjectMapper objectMapper;
    private UUID testCompanyId;
    private UUID testMachineryId;
    private MachineryResponseDTO testMachineryResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(machineryController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        testCompanyId = UUID.randomUUID();
        testMachineryId = UUID.randomUUID();
        testMachineryResponse = new MachineryResponseDTO(
                testMachineryId,
                testCompanyId,
                "SEPI S.A.C.",
                "GEN-CAT-150KW",
                "Grupo Electrógeno Caterpillar 150 kW",
                "DISPONIBLE",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 11, 1),
                OffsetDateTime.now()
        );
    }

    @Nested
    @DisplayName("POST /api/machinery")
    class CreateMachineryTests {

        @Test
        @DisplayName("Should return 201 Created when payload is valid")
        void shouldReturn201WhenValid() throws Exception {
            MachineryCreateDTO request = new MachineryCreateDTO(
                    testCompanyId, "GEN-CAT-150KW", "Grupo Electrógeno Caterpillar 150 kW", "DISPONIBLE",
                    LocalDate.of(2026, 8, 1), LocalDate.of(2026, 11, 1)
            );

            when(machineryService.createMachinery(any(MachineryCreateDTO.class), any())).thenReturn(testMachineryResponse);

            mockMvc.perform(post("/api/machinery")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(testMachineryId.toString())))
                    .andExpect(jsonPath("$.code", is("GEN-CAT-150KW")))
                    .andExpect(jsonPath("$.status", is("DISPONIBLE")));
        }

        @Test
        @DisplayName("Should return 409 Conflict when code already exists")
        void shouldReturn409WhenCodeExists() throws Exception {
            MachineryCreateDTO request = new MachineryCreateDTO(
                    testCompanyId, "GEN-CAT-150KW", "Grupo Electrógeno", "DISPONIBLE", null, null
            );

            when(machineryService.createMachinery(any(MachineryCreateDTO.class), any()))
                    .thenThrow(new DuplicateResourceException("Código ya registrado"));

            mockMvc.perform(post("/api/machinery")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)));
        }
    }

    @Nested
    @DisplayName("PATCH /api/machinery/{id}/status")
    class UpdateStatusTests {

        @Test
        @DisplayName("Should return 200 OK when changing machinery status")
        void shouldReturn200WhenStatusUpdated() throws Exception {
            MachineryStatusUpdateDTO request = new MachineryStatusUpdateDTO("EN_USO");
            MachineryResponseDTO updated = new MachineryResponseDTO(
                    testMachineryId, testCompanyId, "SEPI S.A.C.", "GEN-CAT-150KW",
                    "Grupo Electrógeno Caterpillar 150 kW", "EN_USO",
                    LocalDate.of(2026, 8, 1), LocalDate.of(2026, 11, 1), OffsetDateTime.now()
            );

            when(machineryService.updateStatus(eq(testMachineryId), any(MachineryStatusUpdateDTO.class), any()))
                    .thenReturn(updated);

            mockMvc.perform(patch("/api/machinery/{id}/status", testMachineryId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("EN_USO")));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when status is invalid")
        void shouldReturn400WhenStatusInvalid() throws Exception {
            MachineryStatusUpdateDTO request = new MachineryStatusUpdateDTO("ESTADO_INVENTADO");

            mockMvc.perform(patch("/api/machinery/{id}/status", testMachineryId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));
        }
    }

    @Nested
    @DisplayName("DELETE /api/machinery/{id} (Soft Delete)")
    class DeleteMachineryTests {

        @Test
        @DisplayName("Should return 204 No Content when deleting machinery")
        void shouldReturn204WhenDeleted() throws Exception {
            doNothing().when(machineryService).deleteMachinery(eq(testMachineryId), any());

            mockMvc.perform(delete("/api/machinery/{id}", testMachineryId))
                    .andExpect(status().isNoContent());
        }
    }
}
