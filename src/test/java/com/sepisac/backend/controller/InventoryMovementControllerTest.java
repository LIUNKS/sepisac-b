package com.sepisac.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sepisac.backend.dto.InventoryMovementFilterDTO;
import com.sepisac.backend.dto.InventoryMovementResponseDTO;
import com.sepisac.backend.dto.PageResponseDTO;
import com.sepisac.backend.exception.GlobalExceptionHandler;
import com.sepisac.backend.exception.ResourceNotFoundException;
import com.sepisac.backend.service.InventoryMovementService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryMovementController Web Layer Unit Tests")
class InventoryMovementControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private InventoryMovementService movementService;

    @InjectMocks
    private InventoryMovementController movementController;

    private UUID itemId;
    private UUID movementId;
    private PageResponseDTO<InventoryMovementResponseDTO> mockPageResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(movementController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver())
                .build();

        itemId = UUID.randomUUID();
        movementId = UUID.randomUUID();

        InventoryMovementResponseDTO dto = new InventoryMovementResponseDTO(
                movementId,
                UUID.randomUUID(),
                itemId,
                "SKU-PVC-01",
                "Tubo PVC 1/2",
                UUID.randomUUID(),
                "Almacenero Central",
                "ENTRADA",
                100,
                "Recepción de compra",
                OffsetDateTime.now()
        );

        mockPageResponse = new PageResponseDTO<>(
                List.of(dto),
                0,
                10,
                1L,
                1,
                true,
                true
        );
    }

    @Nested
    @DisplayName("GET /api/inventory/movements")
    class GetMovementsEndpointTests {

        @Test
        @DisplayName("Should return 200 OK with PageResponseDTO when querying movements")
        void shouldReturn200WithMovementsPage() throws Exception {
            when(movementService.getMovementsPaged(any(), any(), any(), any(), any(), eq(0), eq(10), eq("createdAt,desc"), any()))
                    .thenReturn(mockPageResponse);

            mockMvc.perform(get("/api/inventory/movements")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].id", is(movementId.toString())))
                    .andExpect(jsonPath("$.content[0].movementType", is("ENTRADA")))
                    .andExpect(jsonPath("$.content[0].quantityChanged", is(100)))
                    .andExpect(jsonPath("$.content[0].inventoryItemSku", is("SKU-PVC-01")));

            verify(movementService).getMovementsPaged(any(), any(), any(), any(), any(), eq(0), eq(10), eq("createdAt,desc"), any());
        }

        @Test
        @DisplayName("Should return 403 Forbidden when service throws AccessDeniedException")
        void shouldReturn403WhenAccessDenied() throws Exception {
            when(movementService.getMovementsPaged(any(), any(), any(), any(), any(), eq(0), eq(10), eq("createdAt,desc"), any()))
                    .thenThrow(new AccessDeniedException("Acceso denegado"));

            mockMvc.perform(get("/api/inventory/movements"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/inventory/movements/search")
    class QueryMovementsEndpointTests {

        @Test
        @DisplayName("Should return 200 OK with PageResponseDTO when structured search is sent")
        void shouldReturn200WithStructuredSearch() throws Exception {
            InventoryMovementFilterDTO filter = new InventoryMovementFilterDTO();
            filter.setMovementType("ENTRADA");

            when(movementService.queryMovements(any(InventoryMovementFilterDTO.class), any()))
                    .thenReturn(mockPageResponse);

            mockMvc.perform(post("/api/inventory/movements/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(filter)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].movementType", is("ENTRADA")));

            verify(movementService).queryMovements(any(InventoryMovementFilterDTO.class), any());
        }
    }

    @Nested
    @DisplayName("GET /api/inventory/movements/items/{itemId}")
    class GetMovementsByItemIdEndpointTests {

        @Test
        @DisplayName("Should return 200 OK with movements for a specific item")
        void shouldReturn200WithItemMovements() throws Exception {
            when(movementService.getMovementsByItemIdPaged(eq(itemId), eq(0), eq(10), eq("createdAt,desc"), any()))
                    .thenReturn(mockPageResponse);

            mockMvc.perform(get("/api/inventory/movements/items/{itemId}", itemId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].inventoryItemId", is(itemId.toString())));

            verify(movementService).getMovementsByItemIdPaged(eq(itemId), eq(0), eq(10), eq("createdAt,desc"), any());
        }

        @Test
        @DisplayName("Should return 404 Not Found when item does not exist")
        void shouldReturn404WhenItemNotFound() throws Exception {
            UUID unknownId = UUID.randomUUID();
            when(movementService.getMovementsByItemIdPaged(eq(unknownId), eq(0), eq(10), eq("createdAt,desc"), any()))
                    .thenThrow(new ResourceNotFoundException("Ítem de inventario no encontrado"));

            mockMvc.perform(get("/api/inventory/movements/items/{itemId}", unknownId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message", is("Ítem de inventario no encontrado")));
        }
    }
}
