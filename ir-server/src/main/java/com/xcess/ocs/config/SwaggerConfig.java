package com.xcess.ocs.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.*;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("OCS API")
                        .version("1.0")
                        .description("OCS API Documentation"))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication", createAPIKeyScheme()));
    }

    private SecurityScheme createAPIKeyScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .bearerFormat("JWT")
                .scheme("bearer");
    }

    @Bean
    public OpenApiCustomizer sortSchemasNumerically() {
        return openApi -> {
            if (openApi.getComponents() != null && openApi.getComponents().getSchemas() != null) {
                Map<String, Object> schemas = new LinkedHashMap<>();

                // Extract schema names and sort by their leading number
                List<String> sortedSchemaNames = new ArrayList<>(openApi.getComponents().getSchemas().keySet());
                sortedSchemaNames.sort(Comparator.comparingInt(SwaggerConfig::extractLeadingNumber));

                // Rebuild schema map in sorted order
                Map<String, Schema> originalSchemas = openApi.getComponents().getSchemas();
                for (String name : sortedSchemaNames) {
                    schemas.put(name, originalSchemas.get(name));
                }

                openApi.getComponents().setSchemas((Map) schemas);
            }
        };
    }

    // Extract leading numeric value from schema names
    private static int extractLeadingNumber(String str) {
        try {
            return Integer.parseInt(str.split("\\.")[0]); // Split by '.' and parse the first part as an integer
        } catch (Exception e) {
            return Integer.MAX_VALUE; // If parsing fails, place it at the end
        }
    }
}