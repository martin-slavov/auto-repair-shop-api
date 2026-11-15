package com.example.auto_repair_shop_api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Auto Repair Shop API")
                        .version("1.0")
                        .description("REST API for an auto repair shop: customers register vehicles and submit "
                                + "service requests, admins schedule visits and issue invoices, mechanics work on "
                                + "their assigned visits and record the parts used.\n\n"
                                + "**How to authenticate:** call `POST /api/auth/login` with your username and "
                                + "password, copy the `token` from the response, click **Authorize** and paste the "
                                + "token (without the `Bearer ` prefix). Endpoints marked \"Access: public\" need no token."))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter the JWT token obtained from POST /api/auth/login (without the 'Bearer ' prefix)")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}