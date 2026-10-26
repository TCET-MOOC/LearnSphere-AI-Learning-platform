package com.MOOC.OnlineLearningPlatfrom.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Standard list envelope for endpoints added from Wave 1 onwards.
 *
 * <p>Request pages with {@code ?page=0&size=20&sort=field,asc}. The response
 * deliberately mirrors Spring Data's zero-based page contract without leaking
 * {@code Pageable}/{@code Sort} internals into the JSON:</p>
 *
 * <pre>{@code
 * {
 *   "content": [...],
 *   "page": 0,
 *   "size": 20,
 *   "totalElements": 42,
 *   "totalPages": 3
 * }
 * }</pre>
 *
 * <p>Existing list endpoints keep their legacy array shape until the wave that
 * owns them migrates them to this envelope ({@code Docs/PLAN.md} 6.2).</p>
 */
@Schema(name = "PageResponse", description = "Standard zero-based pagination envelope")
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                List.copyOf(page.getContent()),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
