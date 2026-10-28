package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.YouTubePublishService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teacher")
@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
public class YouTubePublishController {

    private final YouTubePublishService publishService;

    public YouTubePublishController(YouTubePublishService publishService) {
        this.publishService = publishService;
    }

    @PostMapping("/media/youtube/connect")
    public ResponseEntity<YouTubePublishService.ConnectResponseDto> connectChannel(
            @AuthenticationPrincipal CustomUserDetails principal) {
        YouTubePublishService.ConnectResponseDto response = publishService.getConnectUrl(principal);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(response);
    }

    @GetMapping("/media/youtube/callback")
    public ResponseEntity<YouTubePublishService.ConnectionDto> callback(
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            @AuthenticationPrincipal CustomUserDetails principal) {
        YouTubePublishService.ConnectionDto connection = publishService.handleCallback(code, state, principal);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(connection);
    }

    @GetMapping("/media/youtube/connection")
    public ResponseEntity<YouTubePublishService.ConnectionDto> getConnection(
            @AuthenticationPrincipal CustomUserDetails principal) {
        YouTubePublishService.ConnectionDto connection = publishService.getConnectionStatus(principal);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(connection);
    }

    @DeleteMapping("/media/youtube/connection")
    public ResponseEntity<Void> disconnectConnection(
            @AuthenticationPrincipal CustomUserDetails principal) {
        publishService.disconnectChannel(principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/contents/{contentId}/youtube-publish")
    public ResponseEntity<YouTubePublishService.PublishJobResponseDto> publishContent(
            @PathVariable Long contentId,
            @RequestBody(required = false) YouTubePublishService.PublishJobRequestDto request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        YouTubePublishService.PublishJobResponseDto job = publishService.startPublish(contentId, request, principal);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(job);
    }

    @GetMapping("/media/youtube/jobs/{jobId}")
    public ResponseEntity<YouTubePublishService.PublishJobResponseDto> getJob(
            @PathVariable Long jobId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        YouTubePublishService.PublishJobResponseDto job = publishService.getJob(jobId, principal);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(job);
    }

    @PostMapping("/media/youtube/jobs/{jobId}/simulate-complete")
    public ResponseEntity<YouTubePublishService.PublishJobResponseDto> simulateComplete(
            @PathVariable Long jobId,
            @RequestParam("youtubeVideoId") String youtubeVideoId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        YouTubePublishService.PublishJobResponseDto job = publishService.simulateComplete(jobId, youtubeVideoId, principal);
        return ResponseEntity.ok(job);
    }

    @PostMapping("/media/youtube/jobs/{jobId}/simulate-error")
    public ResponseEntity<YouTubePublishService.PublishJobResponseDto> simulateError(
            @PathVariable Long jobId,
            @RequestParam("error") String error,
            @AuthenticationPrincipal CustomUserDetails principal) {
        YouTubePublishService.PublishJobResponseDto job = publishService.simulateError(jobId, error, principal);
        return ResponseEntity.ok(job);
    }
}
