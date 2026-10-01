package com.sepisac.backend.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SecurityConfigTest {

    private final JwtTokenFilter jwtTokenFilter = mock(JwtTokenFilter.class);
    private final JwtAuthenticationEntryPoint entryPoint = mock(JwtAuthenticationEntryPoint.class);

    @Test
    @DisplayName("Debe parsear y limpiar orígenes CORS eliminando espacios y barras finales")
    void shouldParseAndNormalizeCorsAllowedOrigins() {
        String originsInput = "http://localhost:5173/ , https://sepisac.vercel.app/ , http://127.0.0.1:3000";
        SecurityConfig config = new SecurityConfig(jwtTokenFilter, entryPoint, originsInput);

        CorsConfigurationSource source = config.corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/projects");
        CorsConfiguration corsConfig = source.getCorsConfiguration(request);

        assertThat(corsConfig).isNotNull();
        assertThat(corsConfig.getAllowedOrigins()).containsExactly(
                "http://localhost:5173",
                "https://sepisac.vercel.app",
                "http://127.0.0.1:3000"
        );
        assertThat(corsConfig.getAllowCredentials()).isTrue();
        assertThat(corsConfig.getAllowedMethods()).contains("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
        assertThat(corsConfig.getAllowedHeaders()).contains("Authorization", "Content-Type");
    }

    @Test
    @DisplayName("Debe manejar cadena nula o vacía de orígenes CORS sin lanzar excepción")
    void shouldHandleNullOrEmptyCorsOrigins() {
        SecurityConfig configNull = new SecurityConfig(jwtTokenFilter, entryPoint, null);
        CorsConfigurationSource source = configNull.corsConfigurationSource();
        CorsConfiguration corsConfig = source.getCorsConfiguration(new MockHttpServletRequest());

        assertThat(corsConfig).isNotNull();
        assertThat(corsConfig.getAllowedOrigins()).isEmpty();
    }
}
