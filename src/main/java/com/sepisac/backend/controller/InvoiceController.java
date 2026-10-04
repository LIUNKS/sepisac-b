package com.sepisac.backend.controller;

import com.sepisac.backend.dto.ErrorResponseDTO;
import com.sepisac.backend.dto.InvoiceCancelDTO;
import com.sepisac.backend.dto.InvoiceFilterDTO;
import com.sepisac.backend.dto.InvoiceResponseDTO;
import com.sepisac.backend.dto.PageResponseDTO;
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
@Tag(name = "Invoicing & Billing", description = "Endpoints para emisión de comprobantes, facturas de venta y gestión de cobranzas")
@SecurityRequirement(name = "bearerAuth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class InvoiceController {

    private final InvoiceService invoiceService;

    @Autowired
    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA', 'ALMACEN', 'TECNICO')")
    @Operation(summary = "Listar facturas con paginación y filtros", 
               description = "Obtiene un listado paginado de facturas con filtros por empresa, estado de cobranza y búsqueda por número o cliente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado paginado de facturas obtenido exitosamente",
                    content = @Content(schema = @Schema(implementation = PageResponseDTO.class)))
    })
    public ResponseEntity<PageResponseDTO<InvoiceResponseDTO>> getInvoices(
            @Parameter(description = "ID de la empresa (opcional para SUPERADMIN)") @RequestParam(required = false) UUID companyId,
            @Parameter(description = "Estado de cobranza (PENDIENTE, PARCIAL, PAGADA, ANULADA)") @RequestParam(required = false) String status,
            @Parameter(description = "Búsqueda por número de factura o cliente") @RequestParam(required = false) String search,
            @Parameter(description = "Número de página (0..N)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Cantidad de elementos por página") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Criterio de ordenamiento (ej. createdAt,desc)") @RequestParam(defaultValue = "createdAt,desc") String sort,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        InvoiceFilterDTO filter = new InvoiceFilterDTO(companyId, status, search, page, size, sort);
        return ResponseEntity.ok(invoiceService.getInvoicesPaged(filter, currentUser));
    }

    @PostMapping("/search")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA', 'ALMACEN', 'TECNICO')")
    @Operation(summary = "Búsqueda avanzada de facturas (Body JSON)", 
               description = "Búsqueda estructurada de facturas vía cuerpo JSON.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resultados de búsqueda obtenidos exitosamente",
                    content = @Content(schema = @Schema(implementation = PageResponseDTO.class)))
    })
    public ResponseEntity<PageResponseDTO<InvoiceResponseDTO>> searchInvoices(
            @RequestBody InvoiceFilterDTO filter,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(invoiceService.getInvoicesPaged(filter, currentUser));
    }

    @PostMapping("/from-quotation/{quotationId}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA')")
    @Operation(summary = "Generar factura desde cotización aprobada", 
               description = "Emite una factura comercial tomando el monto total, moneda y datos del cliente a partir de una cotización aprobada.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Factura generada exitosamente", content = @Content(schema = @Schema(implementation = InvoiceResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Cotización no encontrada", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Regla de negocio: la cotización no está en estado APROBADA", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<InvoiceResponseDTO> createInvoiceFromQuotation(
            @Parameter(description = "ID de la cotización aprobada") @PathVariable UUID quotationId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        InvoiceResponseDTO response = invoiceService.createInvoiceFromQuotation(quotationId, currentUser);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA', 'ALMACEN', 'TECNICO')")
    @Operation(summary = "Listar facturas por estado de cobranza", 
               description = "Retorna un listado filtrado de facturas según su estado (ej. PENDIENTE, PARCIAL, PAGADA, VENCIDA) para gestión rápida de cobranzas.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado de facturas obtenido exitosamente", 
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = InvoiceResponseDTO.class))))
    })
    public ResponseEntity<List<InvoiceResponseDTO>> getInvoicesByStatus(
            @Parameter(description = "Estado de cobranza de la factura", example = "PENDIENTE") @PathVariable String status,
            @Parameter(description = "ID de la empresa (opcional para SUPERADMIN)") @RequestParam(required = false) UUID companyId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<InvoiceResponseDTO> response = invoiceService.getInvoicesByStatus(status, companyId, currentUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA', 'ALMACEN', 'TECNICO')")
    @Operation(summary = "Obtener detalle de factura por ID", description = "Consulta los datos de una factura y su saldo deudor actualizado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Factura encontrada", content = @Content(schema = @Schema(implementation = InvoiceResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Factura no encontrada o no pertenece al tenant", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<InvoiceResponseDTO> getInvoiceById(
            @Parameter(description = "ID de la factura") @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        InvoiceResponseDTO response = invoiceService.getInvoiceById(id, currentUser);
        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/{id}/cancel", method = {RequestMethod.PUT, RequestMethod.POST})
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_EMPRESA', 'GERENCIA')")
    @Operation(summary = "Anular factura emitida", 
               description = "Anula una factura emitida cambiando su estado a ANULADA siempre y cuando no tenga pagos registrados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Factura anulada exitosamente", content = @Content(schema = @Schema(implementation = InvoiceResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Regla de negocio: la factura ya está anulada o tiene pagos registrados", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Factura no encontrada", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<InvoiceResponseDTO> cancelInvoice(
            @Parameter(description = "ID de la factura a anular") @PathVariable UUID id,
            @RequestBody(required = false) InvoiceCancelDTO cancelDTO,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        String reason = cancelDTO != null ? cancelDTO.getReason() : "Anulación administrativa";
        InvoiceResponseDTO response = invoiceService.cancelInvoice(id, reason, currentUser);
        return ResponseEntity.ok(response);
    }
}
