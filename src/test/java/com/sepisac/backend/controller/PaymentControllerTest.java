package com.sepisac.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sepisac.backend.dto.InvoicePaymentCreateDTO;
import com.sepisac.backend.dto.InvoicePaymentResponseDTO;
import com.sepisac.backend.exception.GlobalExceptionHandler;
import com.sepisac.backend.exception.OverpaymentException;
import com.sepisac.backend.exception.ResourceNotFoundException;
import com.sepisac.backend.service.InvoiceService;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentController Web Layer Unit Tests - Sprint 5")
class PaymentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private InvoiceService invoiceService;

    @InjectMocks
    private PaymentController paymentController;

    private ObjectMapper objectMapper;
    private UUID invoiceId;
    private UUID paymentId;
    private InvoicePaymentResponseDTO testPaymentResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(paymentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        invoiceId = UUID.randomUUID();
        paymentId = UUID.randomUUID();

        testPaymentResponse = new InvoicePaymentResponseDTO(
                paymentId, invoiceId, "FAC-2026-001", new BigDecimal("400.00"),
                "TRANSFERENCIA", "OP-123456", OffsetDateTime.now(), "PARCIAL", new BigDecimal("600.00")
        );
    }

    @Test
    @DisplayName("POST /api/v1/invoices/{id}/payments - Éxito 201 registra abono")
    void shouldRegisterPaymentSuccessfully() throws Exception {
        InvoicePaymentCreateDTO dto = new InvoicePaymentCreateDTO(
                new BigDecimal("400.00"), "TRANSFERENCIA", "OP-123456", "PEN", null);

        when(invoiceService.registerPayment(eq(invoiceId), any(InvoicePaymentCreateDTO.class), any()))
                .thenReturn(testPaymentResponse);

        mockMvc.perform(post("/api/v1/invoices/{id}/payments", invoiceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(paymentId.toString())))
                .andExpect(jsonPath("$.amountPaid", is(400.00)))
                .andExpect(jsonPath("$.resultingPaymentStatus", is("PARCIAL")))
                .andExpect(jsonPath("$.remainingBalance", is(600.00)));
    }

    @Test
    @DisplayName("POST /api/v1/invoices/{id}/payments - Falla 422 ante sobrepago (Test 2)")
    void shouldReturnUnprocessableEntityWhenOverpayment() throws Exception {
        InvoicePaymentCreateDTO dto = new InvoicePaymentCreateDTO(
                new BigDecimal("1500.00"), "TRANSFERENCIA", "OP-EXCESS", "PEN", null);

        when(invoiceService.registerPayment(eq(invoiceId), any(InvoicePaymentCreateDTO.class), any()))
                .thenThrow(new OverpaymentException("El abono excede el saldo deudor pendiente"));

        mockMvc.perform(post("/api/v1/invoices/{id}/payments", invoiceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("El abono excede el saldo deudor pendiente"));
    }

    @Test
    @DisplayName("POST /api/v1/invoices/{id}/payments - Falla 400 ante incompatibilidad de divisas (Test 3)")
    void shouldReturnBadRequestWhenCurrencyMismatch() throws Exception {
        InvoicePaymentCreateDTO dto = new InvoicePaymentCreateDTO(
                new BigDecimal("100.00"), "TRANSFERENCIA", "OP-USD", "USD", null);

        when(invoiceService.registerPayment(eq(invoiceId), any(InvoicePaymentCreateDTO.class), any()))
                .thenThrow(new IllegalArgumentException("Incompatibilidad de divisas: factura en PEN y pago en USD"));

        mockMvc.perform(post("/api/v1/invoices/{id}/payments", invoiceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Incompatibilidad de divisas: factura en PEN y pago en USD"));
    }

    @Test
    @DisplayName("GET /api/v1/invoices/{id}/payments - Éxito 200 lista abonos")
    void shouldGetPaymentsByInvoice() throws Exception {
        when(invoiceService.getPaymentsByInvoice(eq(invoiceId), any()))
                .thenReturn(List.of(testPaymentResponse));

        mockMvc.perform(get("/api/v1/invoices/{id}/payments", invoiceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].amountPaid", is(400.00)));
    }
}
