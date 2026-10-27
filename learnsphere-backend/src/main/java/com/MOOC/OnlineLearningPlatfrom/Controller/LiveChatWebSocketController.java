package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Entity.LiveSession;
import com.MOOC.OnlineLearningPlatfrom.Repository.LiveSessionRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Security.OwnershipService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Controller for Live Session chat and reactions over STOMP (W1-09).
 *
 * <p>Enforces server-side identity: client-supplied userId/sender is never trusted,
 * and only users enrolled in the live session's course (or staff/admin) can send messages.</p>
 */
@Controller
public class LiveChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final LiveSessionRepository liveSessionRepository;
    private final OwnershipService ownershipService;

    public LiveChatWebSocketController(SimpMessagingTemplate messagingTemplate,
                                       LiveSessionRepository liveSessionRepository,
                                       OwnershipService ownershipService) {
        this.messagingTemplate = messagingTemplate;
        this.liveSessionRepository = liveSessionRepository;
        this.ownershipService = ownershipService;
    }

    @MessageMapping("/live-sessions/{sessionId}/chat")
    public void sendLiveChatMessage(@DestinationVariable Long sessionId,
                                    @Payload Map<String, Object> payload,
                                    Principal principal) {
        CustomUserDetails user = extractUser(principal);
        verifyEnrolledInSession(sessionId, user);

        // Never trust client-supplied userId or sender!
        payload.put("userId", user.getUser().getUserId());
        payload.put("sender", user.getUser().getFullName());
        payload.put("senderEmail", user.getUsername());
        payload.put("timestamp", LocalDateTime.now().toString());

        messagingTemplate.convertAndSend("/topic/live-sessions/" + sessionId + "/chat", payload);
    }

    @MessageMapping("/live-sessions/{sessionId}/reaction")
    public void sendLiveReaction(@DestinationVariable Long sessionId,
                                 @Payload Map<String, Object> payload,
                                 Principal principal) {
        CustomUserDetails user = extractUser(principal);
        verifyEnrolledInSession(sessionId, user);

        // Never trust client-supplied userId!
        payload.put("userId", user.getUser().getUserId());
        payload.put("timestamp", LocalDateTime.now().toString());

        messagingTemplate.convertAndSend("/topic/live-sessions/" + sessionId + "/reactions", payload);
    }

    private CustomUserDetails extractUser(Principal principal) {
        if (principal instanceof Authentication auth && auth.getPrincipal() instanceof CustomUserDetails user) {
            return user;
        }
        throw new AccessDeniedException("Authentication required to participate in live chat");
    }

    private void verifyEnrolledInSession(Long sessionId, CustomUserDetails user) {
        LiveSession session = liveSessionRepository.findById(sessionId)
                .orElseThrow(() -> new AccessDeniedException("Live session not found: " + sessionId));
        if (session.getCourse() != null) {
            try {
                ownershipService.requireEnrolled(session.getCourse().getId(), user);
            } catch (Exception e) {
                throw new AccessDeniedException("Enrolment required for live session: " + sessionId);
            }
        }
    }
}
