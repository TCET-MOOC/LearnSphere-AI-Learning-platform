package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Service.YouTubeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/teacher/youtube", "/api/youtube"})
public class YouTubeController {

    public record PlaylistImportRequest(String playlistUrl) {}

    private final YouTubeService youtubeService;

    public YouTubeController(YouTubeService youtubeService) {
        this.youtubeService = youtubeService;
    }

    @PostMapping("/import-playlist")
    public ResponseEntity<YouTubeService.PlaylistImportResponse> importPlaylist(@RequestBody PlaylistImportRequest request) {
        YouTubeService.PlaylistImportResponse response = youtubeService.importPlaylist(request.playlistUrl());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/transcript/{videoId}")
    public ResponseEntity<YouTubeService.TranscriptResponse> getTranscript(@PathVariable String videoId) {
        YouTubeService.TranscriptResponse response = youtubeService.getTranscript(videoId);
        return ResponseEntity.ok(response);
    }
}
