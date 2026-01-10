package com.acme.saas.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI api() {
        return new OpenAPI()
            .info(new Info()
                .title("SaaS Starter API")
                .description("OpenAPI for the SaaS starter. Authentication is required via JWT (Bearer token) or API key.")
                .version("0.1.0"))
            // Add security requirement for all endpoints
            .addSecurityItem(new SecurityRequirement()
                .addList("bearerAuth")
                .addList("apiKeyAuth"))
            .components(new Components()
                // Bearer token authentication (Clerk JWT)
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Clerk JWT token. Include the 'Authorization: Bearer <token>' header."))
                // API key authentication (for service-to-service calls)
                .addSecuritySchemes("apiKeyAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.HEADER)
                    .name("X-API-Key")
                    .description("API key for service-to-service calls. Use with X-Tenant-Id header."))
                // Keep X-Tenant-Id for API key auth documentation
                .addParameters("X-Tenant-Id", new Parameter()
                    .in("header")
                    .name("X-Tenant-Id")
                    .description("Tenant identifier for API key auth (slug, e.g., 'acme'). Not needed for JWT auth - tenant is derived from token.")
                    .required(false)
                    .schema(new StringSchema())
                    .example("acme")));
    }
}
