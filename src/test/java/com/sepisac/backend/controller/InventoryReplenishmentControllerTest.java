package com.sepisac.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sepisac.backend.dto.InventoryReplenishmentSettingsRequestDTO;
import com.sepisac.backend.exception.BusinessRuleException;
import com.sepisac.backend.exception.GlobalExceptionHandler;
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

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryReplenishmentController Web Layer Unit Tests")
class InventoryReplenishmentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PurchaseOrderService purchaseOrderService;

    @InjectMocks
    private InventoryReplenishmentController inventoryReplenishmentController;

    private ObjectMapper objectMapper;
    private UUID companyId;
    private UUID itemId;
    private UUID supplierId;

    @BeforeEach
    void setUp() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        objectMapper = new ObjectMapper();

        mockMvc = MockMvcBuilders.standaloneSetup(inventoryReplenishmentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        companyId = UUID.randomUUID();
        itemId = UUID.randomUUID();
        supplierId = UUID.randomUUID();
    }

    @Test
    @DisplayName("PUT /api/v1/inventory-items/{id}/replenishment-settings should update settings and return 200 OK")
    void testUpdateReplenishmentSettings() throws Exception {
        InventoryReplenishmentSettingsRequestDTO request =
                new InventoryReplenishmentSettingsRequestDTO(supplierId, 10, 20);

        doNothing().when(purchaseOrderService).updateReplenishmentSettings(eq(companyId), any(), eq(itemId), any());

        mockMvc.perform(put("/api/v1/inventory-items/{id}/replenishment-settings", itemId)
                        .param("companyId", companyId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Configuración de reposición actualizada exitosamente")));
    }

    @Test
    @DisplayName("PUT /api/v1/inventory-items/{id}/replenishment-settings should return 400 when validation fails (negative minStock)")
    void testUpdateReplenishmentSettingsValidationFailure() throws Exception {
        InventoryReplenishmentSettingsRequestDTO request =
                new InventoryReplenishmentSettingsRequestDTO(supplierId, -5, 20);

        mockMvc.perform(put("/api/v1/inventory-items/{id}/replenishment-settings", itemId)
                        .param("companyId", companyId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
