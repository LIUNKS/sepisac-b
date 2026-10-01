package com.sepisac.backend.controller;

import com.sepisac.backend.dto.ErrorResponseDTO;
import com.sepisac.backend.dto.InvoicePaymentCreateDTO;
import com.sepisac.backend.dto.InvoicePaymentResponseDTO;
import com.sepisac.backend.security.UserPrincipal;
import com.sepisac.backend.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/invoices", "/api/invoices"})
@Tag(name = "Payments & Accounts Receivable", description = "Endpoints para registro de pagos, abonos y gestión de cuentas por cobrar")
@SecurityRequirement(name = "bearerAuth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class PaymentController {

    private final InvoiceService invoiceService;

    @Autowired
    public PaymentController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping("/{id}/payments")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA')")
    @Operation(summary = "Registrar abono o pago a una factura", 
               description = "Registra un nuevo cobro, valida que no exceda el saldo pendiente, comprueba congruencia de divisas y actualiza automáticamente el estado de la factura a PARCIAL o PAGADA.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Pago registrado exitosamente", content = @Content(schema = @Schema(implementation = InvoicePaymentResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Incompatibilidad de divisas o datos inválidos", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Factura no encontrada", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "422", description = "Sobrepago: el monto abonado excede el saldo deudor pendiente", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<InvoicePaymentResponseDTO> registerPayment(
            @Parameter(description = "ID de la factura a abonar") @PathVariable UUID id,
            @Valid @RequestBody InvoicePaymentCreateDTO dto,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        InvoicePaymentResponseDTO response = invoiceService.registerPayment(id, dto, currentUser);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}/payments")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA', 'ALMACEN', 'TECNICO')")
    @Operation(summary = "Consultar historial de pagos de una factura", 
               description = "Retorna todos los abonos y pagos registrados para una factura específica.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado de pagos obtenido", 
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = InvoicePaymentResponseDTO.class)))),
            @ApiResponse(responseCode = "404", description = "Factura no encontrada", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<List<InvoicePaymentResponseDTO>> getPaymentsByInvoice(
            @Parameter(description = "ID de la factura") @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<InvoicePaymentResponseDTO> payments = invoiceService.getPaymentsByInvoice(id, currentUser);
        return ResponseEntity.ok(payments);
    }
}
