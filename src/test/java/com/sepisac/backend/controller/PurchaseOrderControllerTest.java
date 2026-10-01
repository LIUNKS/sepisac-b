package com.sepisac.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sepisac.backend.dto.*;
import com.sepisac.backend.exception.BusinessRuleException;
import com.sepisac.backend.exception.GlobalExceptionHandler;
import com.sepisac.backend.exception.ResourceNotFoundException;
import com.sepisac.backend.service.PurchaseOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("PurchaseOrderController Web Layer Unit Tests")
class PurchaseOrderControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PurchaseOrderService purchaseOrderService;

    @InjectMocks
    private PurchaseOrderController purchaseOrderController;

    private ObjectMapper objectMapper;
    private UUID companyId;
    private UUID orderId;
    private PurchaseOrderResponseDTO testOrderResponse;

    @BeforeEach
    void setUp() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(purchaseOrderController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        companyId = UUID.randomUUID();
        orderId = UUID.randomUUID();

        testOrderResponse = new PurchaseOrderResponseDTO(
                orderId,
                companyId,
                UUID.randomUUID(),
                "ACEROS AREQUIPA S.A.",
                "20100010724",
                "OC-2026-000001",
                "PEN",
                new BigDecimal("1.0000"),
                "PENDIENTE",
                new BigDecimal("500.00"),
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                List.of(new PurchaseOrderDetailResponseDTO(
                        UUID.randomUUID(), UUID.randomUUID(), "MAT-001", "Fierro", 10, new BigDecimal("50.00"), new BigDecimal("500.00")
                ))
        );
    }

    @Test
    @DisplayName("POST /api/v1/purchase-orders/auto-generate should return 201 Created")
    void testAutoGenerateOrders() throws Exception {
        AutoGenerateOrdersResponseDTO responseDTO = new AutoGenerateOrdersResponseDTO(2, 1, 0);
        when(purchaseOrderService.autoGenerateOrders(eq(companyId), any())).thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/purchase-orders/auto-generate")
                        .param("companyId", companyId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ordersCreated", is(2)))
                .andExpect(jsonPath("$.itemsWithoutSupplier", is(1)))
                .andExpect(jsonPath("$.itemsSkippedWithOpenOrder", is(0)));
    }

    @Test
    @DisplayName("POST /api/v1/purchase-orders should create manual order and return 201 Created")
    void testCreateManualOrder() throws Exception {
        CreatePurchaseOrderRequestDTO request = new CreatePurchaseOrderRequestDTO(
                UUID.randomUUID(),
                "PEN",
                new BigDecimal("1.0000"),
                List.of(new PurchaseOrderDetailRequestDTO(UUID.randomUUID(), 5, new BigDecimal("20.00")))
        );

        when(purchaseOrderService.createManualOrder(eq(companyId), any(), any())).thenReturn(testOrderResponse);

        mockMvc.perform(post("/api/v1/purchase-orders")
                        .param("companyId", companyId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber", is("OC-2026-000001")))
                .andExpect(jsonPath("$.status", is("PENDIENTE")));
    }

    @Test
    @DisplayName("GET /api/v1/purchase-orders should return paginated list")
    void testGetOrdersPaged() throws Exception {
        PageResponseDTO<PurchaseOrderResponseDTO> pageResponse = new PageResponseDTO<>(
                List.of(testOrderResponse), 0, 10, 1L, 1, true, true
        );
        when(purchaseOrderService.getOrdersByStatus(eq(companyId), eq("PENDIENTE"), any(Pageable.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/purchase-orders")
                        .param("companyId", companyId.toString())
                        .param("status", "PENDIENTE")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderNumber", is("OC-2026-000001")))
                .andExpect(jsonPath("$.totalElements", is(1)));
    }

    @Test
    @DisplayName("GET /api/v1/purchase-orders/{id} should return order details")
    void testGetOrderById() throws Exception {
        when(purchaseOrderService.getOrderById(companyId, orderId)).thenReturn(testOrderResponse);

        mockMvc.perform(get("/api/v1/purchase-orders/{id}", orderId)
                        .param("companyId", companyId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(orderId.toString())))
                .andExpect(jsonPath("$.orderNumber", is("OC-2026-000001")));
    }

    @Test
    @DisplayName("PUT /api/v1/purchase-orders/{id}/status should cancel order")
    void testCancelOrderStatus() throws Exception {
        testOrderResponse.setStatus("CANCELADA");
        when(purchaseOrderService.cancelOrder(companyId, null, orderId)).thenReturn(testOrderResponse);

        UpdatePurchaseOrderStatusRequestDTO request = new UpdatePurchaseOrderStatusRequestDTO("CANCELADA");

        mockMvc.perform(put("/api/v1/purchase-orders/{id}/status", orderId)
                        .param("companyId", companyId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELADA")));
    }

    @Test
    @DisplayName("PUT /api/v1/purchase-orders/{id}/status with non-PENDIENTE order should return 409 Conflict")
    void testCancelOrderConflict() throws Exception {
        when(purchaseOrderService.cancelOrder(companyId, null, orderId))
                .thenThrow(new BusinessRuleException("PO_INVALID_STATE: Solo se pueden cancelar órdenes en estado PENDIENTE"));

        UpdatePurchaseOrderStatusRequestDTO request = new UpdatePurchaseOrderStatusRequestDTO("CANCELADA");

        mockMvc.perform(put("/api/v1/purchase-orders/{id}/status", orderId)
                        .param("companyId", companyId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("PO_INVALID_STATE: Solo se pueden cancelar órdenes en estado PENDIENTE")));
    }
}
