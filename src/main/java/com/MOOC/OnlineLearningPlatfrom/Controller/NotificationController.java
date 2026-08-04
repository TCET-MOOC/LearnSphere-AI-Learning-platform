package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Dto.NotificationResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.UnreadCountResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponseDto>> getNotifications(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(notificationService.getNotifications(principal));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponseDto> markRead(@PathVariable Long id,
                                                              @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(notificationService.markRead(id, principal));
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal CustomUserDetails principal) {
        notificationService.markAllRead(principal);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponseDto> getUnreadCount(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(new UnreadCountResponseDto(notificationService.getUnreadCount(principal)));
    }
}
