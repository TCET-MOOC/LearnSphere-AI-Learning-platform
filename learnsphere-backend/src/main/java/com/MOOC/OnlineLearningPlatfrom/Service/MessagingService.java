package com.MOOC.OnlineLearningPlatfrom.Service;

import com.MOOC.OnlineLearningPlatfrom.Dto.ConversationResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.MessageResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.SendMessageRequestDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.StartConversationRequestDto;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;

import java.util.List;

public interface MessagingService {
    List<ConversationResponseDto> getConversations(CustomUserDetails principal);
    List<MessageResponseDto> getMessages(Long conversationId, CustomUserDetails principal);
    MessageResponseDto sendMessage(Long conversationId, SendMessageRequestDto request, CustomUserDetails principal);
    ConversationResponseDto startConversation(StartConversationRequestDto request, CustomUserDetails principal);
}
