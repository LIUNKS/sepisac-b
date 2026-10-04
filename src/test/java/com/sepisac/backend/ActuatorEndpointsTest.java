package com.sepisac.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ActuatorEndpointsTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("GET /actuator/health debe responder 200 UP con detalles de componentes (db, diskSpace)")
    void shouldReturnHealthWithDetails() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.db.status").value("UP"))
                .andExpect(jsonPath("$.components.diskSpace.status").value("UP"));
    }

    @Test
    @DisplayName("GET /actuator/metrics debe responder 200 y listar métricas de HikariCP y JVM")
    void shouldReturnActuatorMetrics() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names", not(empty())))
                .andExpect(jsonPath("$.names", hasItem(startsWith("hikaricp.connections"))))
                .andExpect(jsonPath("$.names", hasItem(startsWith("jvm.memory"))));
    }

    @Test
    @DisplayName("GET /actuator/metrics/{name} debe responder métricas de conexiones HikariCP")
    void shouldReturnHikariMetric() throws Exception {
        mockMvc.perform(get("/actuator/metrics/hikaricp.connections.active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("hikaricp.connections.active"));
    }

    @Test
    @DisplayName("GET /actuator/metrics/{name} debe responder métricas de memoria JVM")
    void shouldReturnJvmMetric() throws Exception {
        mockMvc.perform(get("/actuator/metrics/jvm.memory.used"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("jvm.memory.used"));
    }
}
