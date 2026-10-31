package com.MOOC.OnlineLearningPlatfrom.Service;

import com.MOOC.OnlineLearningPlatfrom.Dto.ConversationResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.MessageResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.SendMessageRequestDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.StartConversationRequestDto;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Service for direct participant messaging and conversation management (W5-08).
 *
 * <p><b>Retention Note:</b> Direct messages and conversation threads between participants
 * are retained for academic integrity, audit, and dispute resolution purposes (default 1-year window).
 * Soft-preservation and moderation flags ensure compliance with institutional policies.</p>
 */
public interface MessagingService {
    List<ConversationResponseDto> getConversations(CustomUserDetails principal);
    List<MessageResponseDto> getMessages(Long conversationId, CustomUserDetails principal);
    Page<MessageResponseDto> getMessagesPaginated(Long conversationId, int page, int size, CustomUserDetails principal);
    MessageResponseDto sendMessage(Long conversationId, SendMessageRequestDto request, CustomUserDetails principal);
    ConversationResponseDto startConversation(StartConversationRequestDto request, CustomUserDetails principal);
    ConversationResponseDto blockConversation(Long conversationId, CustomUserDetails principal);
    ConversationResponseDto unblockConversation(Long conversationId, CustomUserDetails principal);
    void reportConversation(Long conversationId, String reason, CustomUserDetails principal);
}
