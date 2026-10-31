package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Dto.NotificationResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.UnreadCountResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.NotificationOutboxService;
import com.MOOC.OnlineLearningPlatfrom.Service.NotificationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private static final String PRIVATE_NO_STORE = "private, no-store";

    private final NotificationService notificationService;
    private final NotificationOutboxService outboxService;

    public NotificationController(NotificationService notificationService,
                                  NotificationOutboxService outboxService) {
        this.notificationService = notificationService;
        this.outboxService = outboxService;
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponseDto>> getNotifications(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, PRIVATE_NO_STORE)
                .body(notificationService.getNotifications(principal));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponseDto> markRead(@PathVariable Long id,
                                                            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, PRIVATE_NO_STORE)
                .body(notificationService.markRead(id, principal));
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal CustomUserDetails principal) {
        notificationService.markAllRead(principal);
        return ResponseEntity.noContent()
                .header(HttpHeaders.CACHE_CONTROL, PRIVATE_NO_STORE)
                .build();
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponseDto> getUnreadCount(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, PRIVATE_NO_STORE)
                .body(new UnreadCountResponseDto(notificationService.getUnreadCount(principal)));
    }

    @PostMapping("/outbox/process")
    public ResponseEntity<Map<String, Object>> processOutbox() {
        int processed = outboxService.processOutbox();
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, PRIVATE_NO_STORE)
                .body(Map.of("processed", processed));
    }
}
