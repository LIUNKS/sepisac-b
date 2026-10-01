package com.sepisac.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sepisac.backend.dto.PurchaseReceptionResponseDTO;
import com.sepisac.backend.dto.ReceptionMovementDTO;
import com.sepisac.backend.exception.BusinessRuleException;
import com.sepisac.backend.exception.GlobalExceptionHandler;
import com.sepisac.backend.exception.ItemUnavailableException;
import com.sepisac.backend.service.PurchaseOrderService;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("PurchaseReceptionController Web Layer Unit Tests")
class PurchaseReceptionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PurchaseOrderService purchaseOrderService;

    @InjectMocks
    private PurchaseReceptionController purchaseReceptionController;

    private ObjectMapper objectMapper;
    private UUID companyId;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(purchaseReceptionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        companyId = UUID.randomUUID();
        orderId = UUID.randomUUID();
    }

    @Test
    @DisplayName("POST /api/v1/purchase-orders/{id}/receptions should receive order and return 200 OK")
    void testReceiveOrderSuccessfully() throws Exception {
        ReceptionMovementDTO movement = new ReceptionMovementDTO(
                UUID.randomUUID(), UUID.randomUUID(), "MAT-001", "Fierro",
                10, 5, 15, new BigDecimal("30.00"), new BigDecimal("35.00")
        );
        PurchaseReceptionResponseDTO responseDTO = new PurchaseReceptionResponseDTO(
                orderId, "OC-2026-000001", "RECIBIDA", OffsetDateTime.now(), List.of(movement)
        );

        when(purchaseOrderService.receiveOrder(eq(companyId), any(), eq(orderId))).thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/purchase-orders/{id}/receptions", orderId)
                        .param("companyId", companyId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("RECIBIDA")))
                .andExpect(jsonPath("$.orderNumber", is("OC-2026-000001")))
                .andExpect(jsonPath("$.movements[0].quantityAdded", is(10)));
    }

    @Test
    @DisplayName("POST /api/v1/purchase-orders/{id}/receptions should return 409 Conflict when not PENDIENTE")
    void testReceiveOrderInvalidState() throws Exception {
        when(purchaseOrderService.receiveOrder(eq(companyId), any(), eq(orderId)))
                .thenThrow(new BusinessRuleException("PO_INVALID_STATE: Solo se pueden recibir órdenes en estado PENDIENTE"));

        mockMvc.perform(post("/api/v1/purchase-orders/{id}/receptions", orderId)
                        .param("companyId", companyId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("PO_INVALID_STATE: Solo se pueden recibir órdenes en estado PENDIENTE")));
    }

    @Test
    @DisplayName("POST /api/v1/purchase-orders/{id}/receptions should return 422 Unprocessable when item deleted")
    void testReceiveOrderItemUnavailable() throws Exception {
        when(purchaseOrderService.receiveOrder(eq(companyId), any(), eq(orderId)))
                .thenThrow(new ItemUnavailableException("PO_ITEM_UNAVAILABLE: El ítem ha sido eliminado"));

        mockMvc.perform(post("/api/v1/purchase-orders/{id}/receptions", orderId)
                        .param("companyId", companyId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", is("PO_ITEM_UNAVAILABLE: El ítem ha sido eliminado")));
    }
}
