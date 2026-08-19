package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Dto.ConversationResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.MessageResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.SendMessageRequestDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.StartConversationRequestDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.Conversation;
import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.Message;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.ConversationRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.MessageRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserAccountRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserRoleRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.MessagingService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessagingServiceImpl implements MessagingService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserAccountRepository userAccountRepository;
    private final CourseRepository courseRepository;
    private final UserRoleRepository userRoleRepository;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    public MessagingServiceImpl(ConversationRepository conversationRepository,
                                 MessageRepository messageRepository,
                                 UserAccountRepository userAccountRepository,
                                 CourseRepository courseRepository,
                                 UserRoleRepository userRoleRepository,
                                 org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userAccountRepository = userAccountRepository;
        this.courseRepository = courseRepository;
        this.userRoleRepository = userRoleRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public List<ConversationResponseDto> getConversations(CustomUserDetails principal) {
        Long userId = principal.getUser().getUserId();
        return conversationRepository.findAllForUser(userId).stream()
                .map(c -> {
                    Long otherUserId = c.getParticipantA() != null && c.getParticipantA().getUserId().equals(userId)
                            ? (c.getParticipantB() != null ? c.getParticipantB().getUserId() : null)
                            : (c.getParticipantA() != null ? c.getParticipantA().getUserId() : null);
                    return ConversationResponseDto.from(c, userId,
                            messageRepository.countByConversation_IdAndReadAtIsNullAndSender_UserIdNot(c.getId(), userId),
                            otherUserId != null ? resolvePrimaryRole(otherUserId) : null);
                })
                .toList();
    }

    private String resolvePrimaryRole(Long userId) {
        List<String> roles = userRoleRepository.findByUser_UserId(userId).stream()
                .map(ur -> ur.getRole().getName())
                .toList();
        if (roles.contains("TEACHER")) return "teacher";
        if (roles.contains("ADMIN")) return "admin";
        if (roles.contains("STUDENT")) return "student";
        return null;
    }

    @Override
    public List<MessageResponseDto> getMessages(Long conversationId, CustomUserDetails principal) {
        Conversation conversation = requireParticipant(conversationId, principal);
        Long userId = principal.getUser().getUserId();

        List<Message> unread = messageRepository.findByConversation_IdAndReadAtIsNullAndSender_UserIdNot(conversationId, userId);
        LocalDateTime now = LocalDateTime.now();
        for (Message m : unread) {
            m.setReadAt(now);
        }
        if (!unread.isEmpty()) {
            messageRepository.saveAll(unread);
        }

        return messageRepository.findByConversation_IdOrderBySentAtAsc(conversationId).stream()
                .map(MessageResponseDto::from)
                .toList();
    }

    @Override
    @Transactional
    public MessageResponseDto sendMessage(Long conversationId, SendMessageRequestDto request, CustomUserDetails principal) {
        Conversation conversation = requireParticipant(conversationId, principal);
        if (request.getText() == null || request.getText().isBlank()) {
            throw new BadRequestException("text is required");
        }

        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(principal.getUser());
        message.setText(request.getText());
        Message saved = messageRepository.save(message);

        conversation.setLastMessage(request.getText());
        conversation.setLastMessageAt(saved.getSentAt());
        conversationRepository.save(conversation);

        MessageResponseDto dto = MessageResponseDto.from(saved);

        // Broadcast to conversation topic
        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, dto);

        // Also broadcast to recipient's private messages queue
        Long currentUserId = principal.getUser().getUserId();
        Long recipientId = (conversation.getParticipantA() != null && conversation.getParticipantA().getUserId().equals(currentUserId))
                ? (conversation.getParticipantB() != null ? conversation.getParticipantB().getUserId() : null)
                : (conversation.getParticipantA() != null ? conversation.getParticipantA().getUserId() : null);
        if (recipientId != null) {
            messagingTemplate.convertAndSend("/topic/user/" + recipientId + "/messages", dto);
        }

        return dto;
    }

    @Override
    public ConversationResponseDto startConversation(StartConversationRequestDto request, CustomUserDetails principal) {
        if (request.getOtherUserId() == null) {
            throw new BadRequestException("otherUserId is required");
        }
        Long currentUserId = principal.getUser().getUserId();
        if (request.getOtherUserId().equals(currentUserId)) {
            throw new BadRequestException("Cannot start a conversation with yourself");
        }

        UserAccount otherUser = userAccountRepository.findById(request.getOtherUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getOtherUserId()));

        Course course = null;
        if (request.getCourseId() != null) {
            course = courseRepository.findById(request.getCourseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.getCourseId()));
        }

        Conversation existing = conversationRepository
                .findExisting(currentUserId, request.getOtherUserId(), request.getCourseId())
                .orElse(null);

        if (existing != null) {
            long unread = messageRepository.countByConversation_IdAndReadAtIsNullAndSender_UserIdNot(existing.getId(), currentUserId);
            return ConversationResponseDto.from(existing, currentUserId, unread);
        }

        Conversation conversation = new Conversation();
        conversation.setParticipantA(principal.getUser());
        conversation.setParticipantB(otherUser);
        conversation.setCourse(course);
        Conversation saved = conversationRepository.save(conversation);
        return ConversationResponseDto.from(saved, currentUserId, 0);
    }

    private Conversation requireParticipant(Long conversationId, CustomUserDetails principal) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));
        Long userId = principal.getUser().getUserId();
        boolean isParticipant = (conversation.getParticipantA() != null && conversation.getParticipantA().getUserId().equals(userId))
                || (conversation.getParticipantB() != null && conversation.getParticipantB().getUserId().equals(userId));
        if (!isParticipant) {
            throw new AccessDeniedException("You are not a participant in this conversation");
        }
        return conversation;
    }
}
