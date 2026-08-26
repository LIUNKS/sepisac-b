package com.sepisac.backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "SEPI S.A.C. ERP Backend API",
                version = "1.0.0",
                description = "Documentación oficial de los servicios REST del ERP de SEPI S.A.C. con arquitectura multi-tenant y autenticación JWT.",
                contact = @Contact(
                        name = "Equipo de Desarrollo SEPI S.A.C.",
                        email = "soporte@sepisac.com"
                ),
                license = @License(
                        name = "Proprietary - SEPI S.A.C."
                )
        ),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        description = "Autenticación JWT. Ingrese el token JWT emitido en el endpoint de login.",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}
