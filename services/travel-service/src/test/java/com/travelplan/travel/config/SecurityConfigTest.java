package com.travelplan.travel.config;

import com.travelplan.shared.security.JwtAuthenticationFilter;
import com.travelplan.shared.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private JwtUtils jwtUtils;

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig(jwtUtils);
    }

    @Test
    void jwtAuthenticationFilter_returnsFilterInstance() {
        JwtAuthenticationFilter filter = securityConfig.jwtAuthenticationFilter();

        assertThat(filter).isNotNull();
    }

    @Test
    void corsConfigurationSource_containsExpectedOriginsMethodsAndHeaders() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        CorsConfiguration config = source.getCorsConfiguration(new MockHttpServletRequest("GET", "/api/v1/travels"));

        assertThat(config).isNotNull();
        assertThat(config.getAllowedOrigins()).contains("http://localhost:3000", "http://localhost:3001", "http://localhost:5173");
        assertThat(config.getAllowedMethods()).contains("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        assertThat(config.getAllowedHeaders()).contains("Authorization", "Content-Type", "X-Request-ID", "X-Correlation-ID");
        assertThat(config.getExposedHeaders()).contains("X-Correlation-ID");
        assertThat(config.getAllowCredentials()).isTrue();
    }
}
