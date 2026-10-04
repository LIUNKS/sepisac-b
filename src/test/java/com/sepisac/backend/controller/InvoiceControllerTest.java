package com.sepisac.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sepisac.backend.dto.InvoiceCancelDTO;
import com.sepisac.backend.dto.InvoiceFilterDTO;
import com.sepisac.backend.dto.InvoiceResponseDTO;
import com.sepisac.backend.dto.PageResponseDTO;
import com.sepisac.backend.exception.BusinessRuleException;
import com.sepisac.backend.exception.GlobalExceptionHandler;
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
import java.time.LocalDate;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("InvoiceController Web Layer Unit Tests - Sprint 5")
class InvoiceControllerTest {

    private MockMvc mockMvc;

    @Mock
    private InvoiceService invoiceService;

    @InjectMocks
    private InvoiceController invoiceController;

    private ObjectMapper objectMapper;
    private UUID quotationId;
    private UUID invoiceId;
    private UUID companyId;
    private InvoiceResponseDTO testInvoiceDTO;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(invoiceController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        quotationId = UUID.randomUUID();
        invoiceId = UUID.randomUUID();
        companyId = UUID.randomUUID();

        testInvoiceDTO = new InvoiceResponseDTO(
                invoiceId, companyId, "SEPI S.A.C.", quotationId, "COT-2026-001",
                UUID.randomUUID(), "PRJ-COT-001", "Minera Antamina", "FAC-2026-001",
                "PEN", new BigDecimal("1000.00"), BigDecimal.ZERO, new BigDecimal("1000.00"),
                "PENDIENTE", LocalDate.now(), LocalDate.now().plusDays(30), OffsetDateTime.now()
        );
    }

    @Test
    @DisplayName("POST /api/v1/invoices/from-quotation/{quotationId} - Éxito 201 crea factura")
    void shouldCreateInvoiceFromQuotationSuccessfully() throws Exception {
        when(invoiceService.createInvoiceFromQuotation(eq(quotationId), any()))
                .thenReturn(testInvoiceDTO);

        mockMvc.perform(post("/api/v1/invoices/from-quotation/{quotationId}", quotationId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(invoiceId.toString())))
                .andExpect(jsonPath("$.invoiceNumber", is("FAC-2026-001")))
                .andExpect(jsonPath("$.totalAmount", is(1000.00)))
                .andExpect(jsonPath("$.paymentStatus", is("PENDIENTE")));
    }

    @Test
    @DisplayName("POST /api/v1/invoices/from-quotation/{quotationId} - Falla 409 cuando cotización no aprobada")
    void shouldReturnConflictWhenQuotationNotApproved() throws Exception {
        when(invoiceService.createInvoiceFromQuotation(eq(quotationId), any()))
                .thenThrow(new BusinessRuleException("Solo se pueden emitir facturas para cotizaciones aprobadas"));

        mockMvc.perform(post("/api/v1/invoices/from-quotation/{quotationId}", quotationId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Solo se pueden emitir facturas para cotizaciones aprobadas"));
    }

    @Test
    @DisplayName("GET /api/v1/invoices/status/{status} - Éxito 200 retorna listado filtrado")
    void shouldGetInvoicesByStatus() throws Exception {
        when(invoiceService.getInvoicesByStatus(eq("PENDIENTE"), any(), any()))
                .thenReturn(List.of(testInvoiceDTO));

        mockMvc.perform(get("/api/v1/invoices/status/{status}", "PENDIENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].invoiceNumber", is("FAC-2026-001")))
                .andExpect(jsonPath("$[0].paymentStatus", is("PENDIENTE")));
    }

    @Test
    @DisplayName("GET /api/v1/invoices/{id} - Éxito 200 retorna detalle")
    void shouldGetInvoiceById() throws Exception {
        when(invoiceService.getInvoiceById(eq(invoiceId), any()))
                .thenReturn(testInvoiceDTO);

        mockMvc.perform(get("/api/v1/invoices/{id}", invoiceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(invoiceId.toString())))
                .andExpect(jsonPath("$.clientName", is("Minera Antamina")));
    }

    @Test
    @DisplayName("GET /api/v1/invoices/{id} - Falla 404 cuando no existe")
    void shouldReturnNotFoundWhenInvoiceDoesNotExist() throws Exception {
        when(invoiceService.getInvoiceById(eq(invoiceId), any()))
                .thenThrow(new ResourceNotFoundException("Factura no encontrada"));

        mockMvc.perform(get("/api/v1/invoices/{id}", invoiceId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Factura no encontrada"));
    }

    @Test
    @DisplayName("GET /api/v1/invoices - Éxito 200 listado paginado")
    void shouldGetInvoicesPagedSuccessfully() throws Exception {
        PageResponseDTO<InvoiceResponseDTO> pageResponse = new PageResponseDTO<>(
                List.of(testInvoiceDTO), 0, 10, 1, 1, true, true
        );

        when(invoiceService.getInvoicesPaged(any(InvoiceFilterDTO.class), any()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/invoices")
                        .param("page", "0")
                        .param("size", "10")
                        .param("status", "PENDIENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].invoiceNumber", is("FAC-2026-001")));
    }

    @Test
    @DisplayName("POST /api/v1/invoices/search - Éxito 200 búsqueda estructurada")
    void shouldSearchInvoicesSuccessfully() throws Exception {
        InvoiceFilterDTO filter = new InvoiceFilterDTO(companyId, "PENDIENTE", "Antamina", 0, 10, "createdAt,desc");
        PageResponseDTO<InvoiceResponseDTO> pageResponse = new PageResponseDTO<>(
                List.of(testInvoiceDTO), 0, 10, 1, 1, true, true
        );

        when(invoiceService.getInvoicesPaged(any(InvoiceFilterDTO.class), any()))
                .thenReturn(pageResponse);

        mockMvc.perform(post("/api/v1/invoices/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filter)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].clientName", is("Minera Antamina")));
    }

    @Test
    @DisplayName("PUT /api/v1/invoices/{id}/cancel - Éxito 200 anula factura")
    void shouldCancelInvoiceSuccessfully() throws Exception {
        InvoiceResponseDTO cancelledDTO = new InvoiceResponseDTO(
                invoiceId, companyId, "SEPI S.A.C.", quotationId, "COT-2026-001",
                null, null, "Minera Antamina", "FAC-2026-001",
                "PEN", new BigDecimal("1000.00"), BigDecimal.ZERO, new BigDecimal("1000.00"),
                "ANULADA", LocalDate.now(), LocalDate.now().plusDays(30), OffsetDateTime.now()
        );

        InvoiceCancelDTO cancelDTO = new InvoiceCancelDTO("Error en monto");

        when(invoiceService.cancelInvoice(eq(invoiceId), eq("Error en monto"), any()))
                .thenReturn(cancelledDTO);

        mockMvc.perform(put("/api/v1/invoices/{id}/cancel", invoiceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus", is("ANULADA")));
    }

    @Test
    @DisplayName("PUT /api/v1/invoices/{id}/cancel - Falla 409 cuando ya anulada o con pagos")
    void shouldReturnConflictWhenCannotCancelInvoice() throws Exception {
        InvoiceCancelDTO cancelDTO = new InvoiceCancelDTO("Cancelación");

        when(invoiceService.cancelInvoice(eq(invoiceId), eq("Cancelación"), any()))
                .thenThrow(new BusinessRuleException("No se puede anular una factura con pagos registrados."));

        mockMvc.perform(put("/api/v1/invoices/{id}/cancel", invoiceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelDTO)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No se puede anular una factura con pagos registrados."));
    }
}
