package com.acme.saas.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI api() {
        return new OpenAPI()
            .info(new Info()
                .title("SaaS Starter API")
                .description("OpenAPI for the SaaS starter")
                .version("0.1.0"))
            .components(new Components()
                .addParameters("X-Tenant-Id", new Parameter()
                    .in("header")
                    .name("X-Tenant-Id")
                    .description("Tenant identifier (slug, e.g., 'acme')")
                    .required(false)
                    .schema(new StringSchema())
                    .example("acme")));
    }
}
