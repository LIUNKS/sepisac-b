package com.sepisac.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sepisac.backend.dto.InventoryItemCreateDTO;
import com.sepisac.backend.dto.InventoryItemResponseDTO;
import com.sepisac.backend.dto.InventoryItemUpdateDTO;
import com.sepisac.backend.exception.DuplicateResourceException;
import com.sepisac.backend.exception.GlobalExceptionHandler;
import com.sepisac.backend.service.InventoryItemService;
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
@DisplayName("InventoryItemController Web Layer Unit Tests")
class InventoryItemControllerTest {

    private MockMvc mockMvc;

    @Mock
    private InventoryItemService inventoryItemService;

    @InjectMocks
    private InventoryItemController inventoryItemController;

    private ObjectMapper objectMapper;
    private UUID testCompanyId;
    private UUID testItemId;
    private InventoryItemResponseDTO testItemResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(inventoryItemController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        testCompanyId = UUID.randomUUID();
        testItemId = UUID.randomUUID();
        testItemResponse = new InventoryItemResponseDTO(
                testItemId,
                testCompanyId,
                "SEPI S.A.C.",
                "TUB-AC-2PULG",
                "Tubo de Acero ASTM A53",
                "Desc",
                50,
                new BigDecimal("120.50"),
                new BigDecimal("185.00"),
                10,
                false,
                OffsetDateTime.now()
        );
    }

    @Nested
    @DisplayName("POST /api/inventory/items")
    class CreateItemTests {

        @Test
        @DisplayName("Should return 201 Created when item payload is valid")
        void shouldReturn201WhenValid() throws Exception {
            InventoryItemCreateDTO request = new InventoryItemCreateDTO(
                    testCompanyId, "TUB-AC-2PULG", "Tubo de Acero ASTM A53", "Desc",
                    50, new BigDecimal("120.50"), new BigDecimal("185.00"), 10
            );

            when(inventoryItemService.createItem(any(InventoryItemCreateDTO.class), any())).thenReturn(testItemResponse);

            mockMvc.perform(post("/api/inventory/items")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(testItemId.toString())))
                    .andExpect(jsonPath("$.sku", is("TUB-AC-2PULG")));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when stock quantity is negative")
        void shouldReturn400WhenStockNegative() throws Exception {
            InventoryItemCreateDTO request = new InventoryItemCreateDTO(
                    testCompanyId, "TUB-AC-2PULG", "Tubo de Acero ASTM A53", "Desc",
                    -5, new BigDecimal("120.50"), new BigDecimal("185.00"), 10
            );

            mockMvc.perform(post("/api/inventory/items")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));
        }

        @Test
        @DisplayName("Should return 409 Conflict when SKU already exists")
        void shouldReturn409WhenSkuExists() throws Exception {
            InventoryItemCreateDTO request = new InventoryItemCreateDTO(
                    testCompanyId, "TUB-AC-2PULG", "Tubo de Acero ASTM A53", "Desc",
                    50, new BigDecimal("120.50"), new BigDecimal("185.00"), 10
            );

            when(inventoryItemService.createItem(any(InventoryItemCreateDTO.class), any()))
                    .thenThrow(new DuplicateResourceException("SKU ya registrado"));

            mockMvc.perform(post("/api/inventory/items")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)));
        }
    }

    @Nested
    @DisplayName("DELETE /api/inventory/items/{id}")
    class DeleteItemTests {

        @Test
        @DisplayName("Should return 204 No Content when deleting item")
        void shouldReturn204WhenDeleted() throws Exception {
            doNothing().when(inventoryItemService).deleteItem(eq(testItemId), any());

            mockMvc.perform(delete("/api/inventory/items/{id}", testItemId))
                    .andExpect(status().isNoContent());
        }
    }
}
