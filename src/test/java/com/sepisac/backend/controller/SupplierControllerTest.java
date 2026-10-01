package com.sepisac.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sepisac.backend.dto.SupplierCreateDTO;
import com.sepisac.backend.dto.SupplierResponseDTO;
import com.sepisac.backend.dto.SupplierUpdateDTO;
import com.sepisac.backend.exception.DuplicateResourceException;
import com.sepisac.backend.exception.GlobalExceptionHandler;
import com.sepisac.backend.exception.ResourceNotFoundException;
import com.sepisac.backend.service.SupplierService;
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

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("SupplierController Web Layer Unit Tests")
class SupplierControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SupplierService supplierService;

    @InjectMocks
    private SupplierController supplierController;

    private ObjectMapper objectMapper;
    private UUID testCompanyId;
    private UUID testSupplierId;
    private SupplierResponseDTO testSupplierResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(supplierController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        testCompanyId = UUID.randomUUID();
        testSupplierId = UUID.randomUUID();
        testSupplierResponse = new SupplierResponseDTO(
                testSupplierId,
                testCompanyId,
                "SEPI S.A.C.",
                "ACEROS AREQUIPA S.A.",
                "20100010724",
                "987654321",
                "ventas@aceros.com",
                OffsetDateTime.now()
        );
    }

    @Nested
    @DisplayName("POST /api/suppliers")
    class CreateSupplierTests {

        @Test
        @DisplayName("Should return 201 Created when payload is valid")
        void shouldReturn201WhenValid() throws Exception {
            SupplierCreateDTO request = new SupplierCreateDTO(
                    testCompanyId, "ACEROS AREQUIPA S.A.", "20100010724", "987654321", "ventas@aceros.com"
            );

            when(supplierService.createSupplier(any(SupplierCreateDTO.class), any())).thenReturn(testSupplierResponse);

            mockMvc.perform(post("/api/suppliers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(testSupplierId.toString())))
                    .andExpect(jsonPath("$.businessName", is("ACEROS AREQUIPA S.A.")))
                    .andExpect(jsonPath("$.ruc", is("20100010724")));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when RUC format is invalid")
        void shouldReturn400WhenRucInvalid() throws Exception {
            SupplierCreateDTO request = new SupplierCreateDTO(
                    testCompanyId, "ACEROS AREQUIPA S.A.", "12345", "987654321", "ventas@aceros.com"
            );

            mockMvc.perform(post("/api/suppliers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));
        }

        @Test
        @DisplayName("Should return 409 Conflict when RUC is duplicated in company")
        void shouldReturn409WhenRucDuplicate() throws Exception {
            SupplierCreateDTO request = new SupplierCreateDTO(
                    testCompanyId, "ACEROS AREQUIPA S.A.", "20100010724", "987654321", "ventas@aceros.com"
            );

            when(supplierService.createSupplier(any(SupplierCreateDTO.class), any()))
                    .thenThrow(new DuplicateResourceException("RUC ya registrado"));

            mockMvc.perform(post("/api/suppliers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)));
        }
    }

    @Nested
    @DisplayName("GET /api/suppliers/{id} and DELETE /api/suppliers/{id}")
    class GetAndDeleteSupplierTests {

        @Test
        @DisplayName("Should return 200 OK with supplier details")
        void shouldReturn200WithSupplier() throws Exception {
            when(supplierService.getSupplierById(eq(testSupplierId), any())).thenReturn(testSupplierResponse);

            mockMvc.perform(get("/api/suppliers/{id}", testSupplierId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(testSupplierId.toString())));
        }

        @Test
        @DisplayName("Should return 204 No Content when soft deleting supplier")
        void shouldReturn204WhenDeleted() throws Exception {
            doNothing().when(supplierService).deleteSupplier(eq(testSupplierId), any());

            mockMvc.perform(delete("/api/suppliers/{id}", testSupplierId))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("GET /api/v1/suppliers/check-ruc should return true when RUC exists")
        void shouldCheckRucExists() throws Exception {
            when(supplierService.checkRucExists(eq("20100010724"), eq(testCompanyId), any())).thenReturn(true);

            mockMvc.perform(get("/api/v1/suppliers/check-ruc")
                            .param("ruc", "20100010724")
                            .param("companyId", testCompanyId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.exists", is(true)));
        }
    }
}
