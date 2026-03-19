package com.domuspacis.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI domusPacisOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Domus Pacis Platform API")
                        .description("Enterprise hospitality management platform for the Catholic Archdiocese of Kigali. " +
                                "Manages accommodations, conference halls, wedding venues, retreat centers, " +
                                "food services, staff, inventory, and financial operations.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Domus Pacis IT Team")
                                .email("it@domuspacis.org")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .name("bearerAuth")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
