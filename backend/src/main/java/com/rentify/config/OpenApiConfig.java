package com.rentify.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI rentifyOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Rentify - Campus Equipment Sharing API")
                        .description("High-performance Spring Boot monolithic backend for peer-to-peer campus rentals, equipment listings, real-time messaging, and reputation management.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Rentify Engineering Team")
                                .email("support@rentify.campus"))
                        .license(new License().name("MIT License").url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:4000").description("Local Development Server"),
                        new Server().url("http://localhost:8080").description("Alternative Port Server")
                ))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter your JWT token obtained from `/api/auth/login` or `/api/auth/register` (e.g. `eyJhbGciOi...`)")));
    }
}
