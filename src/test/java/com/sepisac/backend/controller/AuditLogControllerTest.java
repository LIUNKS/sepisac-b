package com.sepisac.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sepisac.backend.dto.AuditLogResponseDTO;
import com.sepisac.backend.dto.AuditLogUserDTO;
import com.sepisac.backend.dto.PageResponseDTO;
import com.sepisac.backend.exception.GlobalExceptionHandler;
import com.sepisac.backend.exception.ResourceNotFoundException;
import com.sepisac.backend.security.UserPrincipal;
import com.sepisac.backend.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

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
@DisplayName("AuditLogController Unit Tests")
class AuditLogControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AuditLogController auditLogController;

    private UUID companyId;
    private UserPrincipal testUser;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        testUser = new UserPrincipal(
                UUID.randomUUID(),
                "gerente@sepisac.com",
                "gerente",
                "password",
                "Gerente General",
                companyId,
                List.of(new SimpleGrantedAuthority("ROLE_GERENCIA")),
                true
        );

        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().isAssignableFrom(UserPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest,
                                          WebDataBinderFactory binderFactory) {
                return testUser;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(auditLogController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(authPrincipalResolver)
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/audit-logs - returns 200 OK with paged audit logs")
    void testGetAuditLogs() throws Exception {
        AuditLogResponseDTO dto = new AuditLogResponseDTO(
                UUID.randomUUID(),
                new AuditLogUserDTO(testUser.getId(), "gerente", "gerente@sepisac.com", "Gerente General"),
                "APPROVE",
                "QUOTATIONS",
                UUID.randomUUID(),
                "Cotización aprobada",
                Collections.emptyMap(),
                Collections.emptyMap(),
                OffsetDateTime.now()
        );

        PageResponseDTO<AuditLogResponseDTO> pageResponse = new PageResponseDTO<>(
                List.of(dto), 0, 20, 1, 1, true, true
        );

        when(auditLogService.getAuditLogs(eq(companyId), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/audit-logs")
                        .param("module", "QUOTATIONS")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action", is("APPROVE")))
                .andExpect(jsonPath("$.content[0].moduleAffected", is("QUOTATIONS")))
                .andExpect(jsonPath("$.totalElements", is(1)));
    }

    @Test
    @DisplayName("GET /api/v1/audit-logs/{module}/{entityId} - returns 200 OK with entity audit trail")
    void testGetEntityAuditTrail() throws Exception {
        UUID entityId = UUID.randomUUID();
        AuditLogResponseDTO dto = new AuditLogResponseDTO(
                UUID.randomUUID(),
                null,
                "CREATE",
                "QUOTATIONS",
                entityId,
                "Cotización creada",
                Collections.emptyMap(),
                Collections.emptyMap(),
                OffsetDateTime.now()
        );

        when(auditLogService.getEntityAuditTrail(companyId, "QUOTATIONS", entityId))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/audit-logs/QUOTATIONS/{entityId}", entityId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action", is("CREATE")))
                .andExpect(jsonPath("$[0].entityId", is(entityId.toString())));
    }

    @Test
    @DisplayName("GET /api/v1/audit-logs/{module}/{entityId} - returns 404 when not found")
    void testGetEntityAuditTrailNotFound() throws Exception {
        UUID entityId = UUID.randomUUID();

        when(auditLogService.getEntityAuditTrail(companyId, "QUOTATIONS", entityId))
                .thenThrow(new ResourceNotFoundException("AUDIT_ENTITY_NOT_FOUND: No se encontraron registros de auditoría para la entidad " + entityId));

        mockMvc.perform(get("/api/v1/audit-logs/QUOTATIONS/{entityId}", entityId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("AUDIT_ENTITY_NOT_FOUND: No se encontraron registros de auditoría para la entidad " + entityId)));
    }

    @Test
    @DisplayName("PUT /api/v1/audit-logs - returns 405 Method Not Allowed")
    void testPutAuditLogsBlocked() throws Exception {
        mockMvc.perform(put("/api/v1/audit-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("PATCH /api/v1/audit-logs/{module}/{entityId} - returns 405 Method Not Allowed")
    void testPatchAuditLogsBlocked() throws Exception {
        UUID entityId = UUID.randomUUID();
        mockMvc.perform(patch("/api/v1/audit-logs/QUOTATIONS/{entityId}", entityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("DELETE /api/v1/audit-logs/{id} - returns 405 Method Not Allowed")
    void testDeleteAuditLogsBlocked() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(delete("/api/v1/audit-logs/{id}", id))
                .andExpect(status().isMethodNotAllowed());
    }
}
