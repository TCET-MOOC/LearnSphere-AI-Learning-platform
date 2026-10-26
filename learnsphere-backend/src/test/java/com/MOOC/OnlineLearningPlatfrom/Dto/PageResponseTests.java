package com.MOOC.OnlineLearningPlatfrom.Dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResponseTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldMapASpringPageOntoTheDocumentedEnvelope() {
        Page<String> page = new PageImpl<>(List.of("c", "d"), PageRequest.of(1, 2), 5);

        PageResponse<String> response = PageResponse.from(page);

        assertThat(response.content()).containsExactly("c", "d");
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(5);
        assertThat(response.totalPages()).isEqualTo(3);
    }

    @Test
    void shouldSerializeExactlyTheDocumentedFields() {
        PageResponse<String> response = PageResponse.from(
                new PageImpl<>(List.of("c", "d"), PageRequest.of(1, 2), 5));

        JsonNode json = objectMapper.valueToTree(response);

        assertThat(json.fieldNames()).toIterable()
                .containsExactly("content", "page", "size", "totalElements", "totalPages");
        assertThat(json.get("page").asInt()).isEqualTo(1);
        assertThat(json.get("size").asInt()).isEqualTo(2);
        assertThat(json.get("totalElements").asLong()).isEqualTo(5);
        assertThat(json.get("totalPages").asInt()).isEqualTo(3);
        assertThat(json.get("content")).hasSize(2);
    }

    @Test
    void shouldReportZeroPagesForAnEmptyPage() {
        PageResponse<String> response = PageResponse.from(
                new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        assertThat(response.content()).isEmpty();
        assertThat(response.page()).isZero();
        assertThat(response.totalElements()).isZero();
        assertThat(response.totalPages()).isZero();
    }
}
