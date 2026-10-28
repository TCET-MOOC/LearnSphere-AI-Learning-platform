package com.MOOC.OnlineLearningPlatfrom.Service;

import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

public interface MediaPlaybackService {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record PlaybackDescriptorDto(
            Long contentId,
            String title,
            String contentType,
            String sourceType,
            Integer durationSeconds,
            String mimeType,
            String providerVideoId,
            String embedUrl,
            String streamUrl,
            Instant expiresAt
    ) {}

    PlaybackDescriptorDto getPlaybackDescriptor(Long contentId, CustomUserDetails principal);

    void validateStreamSignature(Long contentId, Long userId, long exp, String sig);

    ResponseEntity<?> streamVideo(Long contentId, Long userId, long exp, String sig, String rangeHeader);
}
