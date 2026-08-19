package com.MOOC.OnlineLearningPlatfrom.Controller;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.Map;

@Controller
public class LiveChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    public LiveChatWebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/live-sessions/{sessionId}/chat")
    public void sendLiveChatMessage(@DestinationVariable Long sessionId, @Payload Map<String, Object> payload) {
        payload.put("timestamp", LocalDateTime.now().toString());
        messagingTemplate.convertAndSend("/topic/live-sessions/" + sessionId + "/chat", payload);
    }

    @MessageMapping("/live-sessions/{sessionId}/reaction")
    public void sendLiveReaction(@DestinationVariable Long sessionId, @Payload Map<String, Object> payload) {
        messagingTemplate.convertAndSend("/topic/live-sessions/" + sessionId + "/reactions", payload);
    }
}
