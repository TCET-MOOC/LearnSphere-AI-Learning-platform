package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Dto.LiveSessionRequestDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.LiveSessionResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.LiveSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teacher/live-sessions")
public class LiveSessionController {

    private final LiveSessionService liveSessionService;

    public LiveSessionController(LiveSessionService liveSessionService) {
        this.liveSessionService = liveSessionService;
    }

    @GetMapping
    public ResponseEntity<List<LiveSessionResponseDto>> getMySessions(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(liveSessionService.getMySessions(principal));
    }

    @PostMapping
    public ResponseEntity<LiveSessionResponseDto> create(@RequestBody LiveSessionRequestDto request,
                                                           @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(liveSessionService.create(request, principal));
    }

    @PutMapping("/{id}/start")
    public ResponseEntity<LiveSessionResponseDto> start(@PathVariable Long id,
                                                          @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(liveSessionService.start(id, principal));
    }

    @PutMapping("/{id}/end")
    public ResponseEntity<LiveSessionResponseDto> end(@PathVariable Long id,
                                                        @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(liveSessionService.end(id, principal));
    }
}
