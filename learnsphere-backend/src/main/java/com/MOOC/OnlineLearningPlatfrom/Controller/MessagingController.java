package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Dto.ConversationResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.MessageResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.SendMessageRequestDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.StartConversationRequestDto;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.MessagingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
public class MessagingController {

    private final MessagingService messagingService;

    public MessagingController(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @GetMapping
    public ResponseEntity<List<ConversationResponseDto>> getConversations(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(messagingService.getConversations(principal));
    }

    @PostMapping
    public ResponseEntity<ConversationResponseDto> startConversation(@RequestBody StartConversationRequestDto request,
                                                                       @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(messagingService.startConversation(request, principal));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<List<MessageResponseDto>> getMessages(@PathVariable Long id,
                                                                  @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(messagingService.getMessages(id, principal));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<MessageResponseDto> sendMessage(@PathVariable Long id,
                                                            @RequestBody SendMessageRequestDto request,
                                                            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(messagingService.sendMessage(id, request, principal));
    }
}
