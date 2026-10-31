package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Dto.ConversationResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.MessageResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.SendMessageRequestDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.StartConversationRequestDto;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.MessagingService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for direct messaging between students and instructors (W5-08).
 * Enforces participant authorization, IDOR protection (404), pagination,
 * block/unblock controls, reporting, and private, no-store caching.
 */
@RestController
@RequestMapping("/api/conversations")
public class MessagingController {

    private final MessagingService messagingService;

    public MessagingController(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @GetMapping
    public ResponseEntity<List<ConversationResponseDto>> getConversations(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(messagingService.getConversations(principal));
    }

    @PostMapping
    public ResponseEntity<ConversationResponseDto> startConversation(@RequestBody StartConversationRequestDto request,
                                                                       @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(messagingService.startConversation(request, principal));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<List<MessageResponseDto>> getMessages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Page<MessageResponseDto> paged = messagingService.getMessagesPaginated(id, page, size, principal);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .header("X-Total-Count", String.valueOf(paged.getTotalElements()))
                .header("X-Page-Number", String.valueOf(paged.getNumber()))
                .header("X-Page-Size", String.valueOf(paged.getSize()))
                .header("X-Total-Pages", String.valueOf(paged.getTotalPages()))
                .body(paged.getContent());
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<MessageResponseDto> sendMessage(@PathVariable Long id,
                                                            @RequestBody SendMessageRequestDto request,
                                                            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(messagingService.sendMessage(id, request, principal));
    }

    @PostMapping("/{id}/block")
    public ResponseEntity<ConversationResponseDto> blockConversation(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(messagingService.blockConversation(id, principal));
    }

    @PostMapping("/{id}/unblock")
    public ResponseEntity<ConversationResponseDto> unblockConversation(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(messagingService.unblockConversation(id, principal));
    }

    @PostMapping("/{id}/report")
    public ResponseEntity<Void> reportConversation(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails principal) {
        String reason = body != null ? body.get("reason") : "SPAM";
        messagingService.reportConversation(id, reason, principal);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .build();
    }
}
