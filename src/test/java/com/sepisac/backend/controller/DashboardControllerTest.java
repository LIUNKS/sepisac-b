package com.sepisac.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sepisac.backend.dto.*;
import com.sepisac.backend.exception.GlobalExceptionHandler;
import com.sepisac.backend.security.UserPrincipal;
import com.sepisac.backend.service.DashboardService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardController Unit Tests")
class DashboardControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private DashboardController dashboardController;

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

        mockMvc = MockMvcBuilders.standaloneSetup(dashboardController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(authPrincipalResolver)
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/dashboard/kpis/commercial-cycle - returns 200 OK with KPIs")
    void testGetCommercialCycleKpi() throws Exception {
        CommercialCycleKpiResponseDTO kpi = new CommercialCycleKpiResponseDTO(
                new StageKpiDTO(new BigDecimal("3.50"), 2),
                new StageKpiDTO(new BigDecimal("5.00"), 2),
                new StageKpiDTO(new BigDecimal("10.00"), 2),
                new TotalKpiDTO(new BigDecimal("18.50"), 2)
        );

        when(dashboardService.getCommercialCycleKpi(eq(companyId), any(), any())).thenReturn(kpi);

        mockMvc.perform(get("/api/v1/dashboard/kpis/commercial-cycle")
                        .param("from", "2026-01-01")
                        .param("to", "2026-03-31")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creationToApproval.averageDays", is(3.50)))
                .andExpect(jsonPath("$.creationToApproval.sampleSize", is(2)))
                .andExpect(jsonPath("$.total.averageDays", is(18.50)));
    }

    @Test
    @DisplayName("GET /api/v1/dashboard/revenue - returns 200 OK with revenue series and totals")
    void testGetRevenueKpi() throws Exception {
        RevenueKpiResponseDTO revenue = new RevenueKpiResponseDTO(
                "month",
                List.of(new RevenueSeriesDTO("2026-02", "PEN", new BigDecimal("1500.00"), 2)),
                List.of(new CurrencyTotalDTO("PEN", new BigDecimal("1500.00"), 2))
        );

        when(dashboardService.getRevenueKpi(eq(companyId), any(), any(), eq("month"))).thenReturn(revenue);

        mockMvc.perform(get("/api/v1/dashboard/revenue")
                        .param("groupBy", "month")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupBy", is("month")))
                .andExpect(jsonPath("$.series[0].currency", is("PEN")))
                .andExpect(jsonPath("$.totalsByCurrency[0].totalAmount", is(1500.00)));
    }

    @Test
    @DisplayName("GET /api/v1/dashboard/receivables - returns 200 OK with receivables status groups and paid summary")
    void testGetReceivablesKpi() throws Exception {
        ReceivablesKpiResponseDTO response = new ReceivablesKpiResponseDTO(
                List.of(new ReceivableStatusGroupDTO("VENCIDA", "PEN", new BigDecimal("700.00"), 1)),
                new PaidSummaryDTO(1, List.of(new CurrencyTotalDTO("PEN", new BigDecimal("500.00"), 1)), new BigDecimal("500.00"), "PEN")
        );

        when(dashboardService.getReceivablesKpi(companyId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/dashboard/receivables")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusGroups[0].status", is("VENCIDA")))
                .andExpect(jsonPath("$.statusGroups[0].totalBalance", is(700.00)))
                .andExpect(jsonPath("$.paidSummary.invoiceCount", is(1)));
    }

    @Test
    @DisplayName("GET /api/v1/dashboard/inventory-alerts - returns 200 OK with page of alerts")
    void testGetInventoryAlerts() throws Exception {
        InventoryAlertResponseDTO alert = new InventoryAlertResponseDTO(
                UUID.randomUUID(), "MAT-01", "Tubo PVC", 2, 10, 8,
                Collections.emptyList(), 0, false
        );
        PageResponseDTO<InventoryAlertResponseDTO> pageResponse = new PageResponseDTO<>(
                List.of(alert), 0, 20, 1, 1, true, true
        );

        when(dashboardService.getInventoryAlerts(eq(companyId), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/dashboard/inventory-alerts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sku", is("MAT-01")))
                .andExpect(jsonPath("$.totalElements", is(1)));
    }
}
