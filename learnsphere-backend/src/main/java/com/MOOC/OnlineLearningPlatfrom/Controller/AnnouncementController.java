package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Dto.AnnouncementRequestDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.AnnouncementResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.AnnouncementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @GetMapping
    public ResponseEntity<List<AnnouncementResponseDto>> getAnnouncements(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(announcementService.getAnnouncements(principal));
    }

    @PostMapping
    public ResponseEntity<AnnouncementResponseDto> createAnnouncement(@RequestBody AnnouncementRequestDto request,
                                                                        @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(announcementService.createAnnouncement(request, principal));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AnnouncementResponseDto> updateAnnouncement(@PathVariable Long id,
                                                                        @RequestBody AnnouncementRequestDto request,
                                                                        @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(announcementService.updateAnnouncement(id, request, principal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAnnouncement(@PathVariable Long id,
                                                    @AuthenticationPrincipal CustomUserDetails principal) {
        announcementService.deleteAnnouncement(id, principal);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/pin")
    public ResponseEntity<AnnouncementResponseDto> togglePin(@PathVariable Long id,
                                                               @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(announcementService.togglePin(id, principal));
    }
}
