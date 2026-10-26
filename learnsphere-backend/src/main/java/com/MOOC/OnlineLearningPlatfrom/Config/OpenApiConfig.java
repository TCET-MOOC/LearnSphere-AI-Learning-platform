package com.MOOC.OnlineLearningPlatfrom.Config;

import com.MOOC.OnlineLearningPlatfrom.Dto.PageResponse;
import com.MOOC.OnlineLearningPlatfrom.Exception.ProblemResponse;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Publishes the API contract at {@code /v3/api-docs}.
 *
 * <p>The shared {@code Problem} and {@code PageResponse} schemas are added even
 * before the first Wave 1 endpoint uses them, so generated clients see the
 * project-wide conventions alongside the endpoint schemas.</p>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI learnSphereOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("LearnSphere AI API")
                        .version("0.1.0")
                        .description("""
                                LearnSphere AI REST API. Errors use RFC 9457 application/problem+json
                                with a stable code and the request traceId. New list endpoints return
                                {content, page, size, totalElements, totalPages}.

                                Protected endpoints require an Authorization: Bearer <access token>
                                header. Public endpoints are marked individually in the endpoint table.
                                """))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }

    @Bean
    public OpenApiCustomizer sharedApiSchemas() {
        return openApi -> {
            Components components = openApi.getComponents() != null
                    ? openApi.getComponents()
                    : new Components();
            addSchema(components, "Problem", ProblemResponse.class);
            addSchema(components, "PageResponse", PageResponse.class);
            openApi.setComponents(components);
        };
    }

    private static void addSchema(Components components, String name, Class<?> type) {
        ResolvedSchema resolved = ModelConverters.getInstance().readAllAsResolvedSchema(type);
        if (resolved.referencedSchemas != null) {
            resolved.referencedSchemas.forEach(components::addSchemas);
        }
        if (resolved.schema != null) {
            components.addSchemas(name, resolved.schema);
        }
    }
}
