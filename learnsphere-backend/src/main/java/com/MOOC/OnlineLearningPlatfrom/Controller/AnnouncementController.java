package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Dto.AnnouncementRequestDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.AnnouncementResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.AnnouncementService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for platform and course-scoped announcements (W5-05, A-17, [E-F13]).
 */
@RestController
@RequestMapping("/api")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @GetMapping("/announcements")
    public ResponseEntity<List<AnnouncementResponseDto>> getAnnouncements(
            @RequestParam(required = false) Long courseId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(announcementService.getAnnouncements(principal, courseId));
    }

    @GetMapping("/courses/{courseId}/announcements")
    public ResponseEntity<List<AnnouncementResponseDto>> getCourseAnnouncements(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(announcementService.getCourseAnnouncements(courseId, principal));
    }

    @PostMapping("/announcements")
    public ResponseEntity<AnnouncementResponseDto> createAnnouncement(
            @RequestBody AnnouncementRequestDto request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(announcementService.createAnnouncement(request, principal));
    }

    @PutMapping("/announcements/{id}")
    public ResponseEntity<AnnouncementResponseDto> updateAnnouncement(
            @PathVariable Long id,
            @RequestBody AnnouncementRequestDto request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(announcementService.updateAnnouncement(id, request, principal));
    }

    @DeleteMapping("/announcements/{id}")
    public ResponseEntity<Void> deleteAnnouncement(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        announcementService.deleteAnnouncement(id, principal);
        return ResponseEntity.noContent()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .build();
    }

    @PutMapping("/announcements/{id}/pin")
    public ResponseEntity<AnnouncementResponseDto> togglePin(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(announcementService.togglePin(id, principal));
    }
}
