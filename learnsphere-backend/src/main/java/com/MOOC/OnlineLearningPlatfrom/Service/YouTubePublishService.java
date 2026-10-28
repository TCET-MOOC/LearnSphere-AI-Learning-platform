package com.MOOC.OnlineLearningPlatfrom.Service;

import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

public interface YouTubePublishService {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ConnectResponseDto(
            String authUrl,
            String state
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ConnectionDto(
            boolean connected,
            String channelId,
            String channelTitle,
            LocalDateTime connectedAt
    ) {}

    record PublishJobRequestDto(
            String idempotencyKey,
            String privacyStatus
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record PublishJobResponseDto(
            Long jobId,
            Long contentId,
            String status,
            String youtubeVideoId,
            String errorMessage,
            LocalDateTime createdAt
    ) {}

    ConnectResponseDto getConnectUrl(CustomUserDetails principal);

    ConnectionDto handleCallback(String code, String state, CustomUserDetails principal);

    ConnectionDto getConnectionStatus(CustomUserDetails principal);

    void disconnectChannel(CustomUserDetails principal);

    PublishJobResponseDto startPublish(Long contentId, PublishJobRequestDto request, CustomUserDetails principal);

    PublishJobResponseDto getJob(Long jobId, CustomUserDetails principal);

    PublishJobResponseDto simulateComplete(Long jobId, String youtubeVideoId, CustomUserDetails principal);

    PublishJobResponseDto simulateError(Long jobId, String errorMessage, CustomUserDetails principal);
}
