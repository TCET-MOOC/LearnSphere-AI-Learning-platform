package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Entity.LiveSession;
import com.MOOC.OnlineLearningPlatfrom.Repository.LiveSessionRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Security.OwnershipService;
import com.MOOC.OnlineLearningPlatfrom.Service.ProfanityFilterService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller for Live Session chat and reactions over STOMP (W1-09).
 *
 * <p>Enforces server-side identity: client-supplied userId/sender is never trusted,
 * and only users enrolled in the live session's course (or staff/admin) can send messages.</p>
 */
@Controller
public class LiveChatWebSocketController {

    private static final int MAX_CHAT_LENGTH = 1000;

    private final SimpMessagingTemplate messagingTemplate;
    private final LiveSessionRepository liveSessionRepository;
    private final OwnershipService ownershipService;
    private final ProfanityFilterService profanityFilterService;

    public LiveChatWebSocketController(SimpMessagingTemplate messagingTemplate,
                                       LiveSessionRepository liveSessionRepository,
                                       OwnershipService ownershipService,
                                       ProfanityFilterService profanityFilterService) {
        this.messagingTemplate = messagingTemplate;
        this.liveSessionRepository = liveSessionRepository;
        this.ownershipService = ownershipService;
        this.profanityFilterService = profanityFilterService;
    }

    @MessageMapping("/live-sessions/{sessionId}/chat")
    public void sendLiveChatMessage(@DestinationVariable Long sessionId,
                                    @Payload Map<String, Object> payload,
                                    Principal principal) {
        CustomUserDetails user = extractUser(principal);
        LiveSession session = verifyEnrolledInSession(sessionId, user);

        // The client supplies only the text (and whether it is a question). Identity, the
        // faculty badge and moderation are decided here; nothing else from the payload is relayed.
        Object rawText = payload != null ? payload.get("text") : null;
        String text = rawText == null ? "" : rawText.toString().trim();
        if (text.isEmpty()) {
            return;
        }
        if (text.length() > MAX_CHAT_LENGTH) {
            text = text.substring(0, MAX_CHAT_LENGTH);
        }
        ProfanityFilterService.CensorResult censored = profanityFilterService.censor(text);
        boolean isTeacher = session.getCourse() != null
                ? ownershipService.isCourseStaff(session.getCourse(), user)
                : ownershipService.isAdmin(user);

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("userId", user.getUser().getUserId());
        message.put("sender", user.getUser().getFullName());
        message.put("isTeacher", isTeacher);
        message.put("text", censored.maskedText());
        message.put("isQuestion", Boolean.TRUE.equals(payload.get("isQuestion")));
        message.put("isCensored", !censored.isClean());
        message.put("timestamp", LocalDateTime.now().toString());

        messagingTemplate.convertAndSend("/topic/live-sessions/" + sessionId + "/chat", message);
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

    private LiveSession verifyEnrolledInSession(Long sessionId, CustomUserDetails user) {
        LiveSession session = liveSessionRepository.findById(sessionId)
                .orElseThrow(() -> new AccessDeniedException("Live session not found: " + sessionId));
        if (session.getCourse() != null) {
            try {
                ownershipService.requireEnrolled(session.getCourse().getId(), user);
            } catch (Exception e) {
                throw new AccessDeniedException("Enrolment required for live session: " + sessionId);
            }
        }
        return session;
    }
}
