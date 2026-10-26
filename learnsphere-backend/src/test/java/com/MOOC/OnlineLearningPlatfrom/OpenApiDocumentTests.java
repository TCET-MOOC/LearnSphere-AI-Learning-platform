package com.MOOC.OnlineLearningPlatfrom;

import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Generates and validates the OpenAPI document that CI publishes.
 *
 * <p>The test is the generation step: running the normal backend suite writes
 * {@code target/openapi.json}, so CI cannot drift from the annotations. It
 * also proves that the shared error and pagination schemas are present before
 * the first Wave 1 endpoint starts using them.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocumentTests extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldPublishTheOpenApiContractAndWriteItForCi() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.openapi").value(startsWith("3.")))
                .andExpect(jsonPath("$.info.title").value("LearnSphere AI API"))
                .andExpect(jsonPath("$.paths['/api/auth/login'].post").exists())
                .andExpect(jsonPath("$.paths['/api/courses/{id}'].get").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth").exists())
                .andExpect(jsonPath("$.components.schemas.Problem.properties.code").exists())
                .andExpect(jsonPath("$.components.schemas.Problem.properties.traceId").exists())
                .andExpect(jsonPath("$.components.schemas.PageResponse.properties.content").exists())
                .andExpect(jsonPath("$.components.schemas.PageResponse.properties.totalElements").exists())
                .andReturn();

        Path output = Path.of("target", "openapi.json");
        Files.createDirectories(output.getParent());
        Files.writeString(output, result.getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertThat(output).exists().isNotEmptyFile();
        assertThat(Files.readString(output)).contains("\"openapi\"");
    }
}
