package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Dto.ConversationResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.MessageResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.SendMessageRequestDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.StartConversationRequestDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.Conversation;
import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.FlaggedContent;
import com.MOOC.OnlineLearningPlatfrom.Entity.Message;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.*;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Security.OwnershipService;
import com.MOOC.OnlineLearningPlatfrom.Service.MessagingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Story W5-08: Direct messaging implementation with pagination, participant authorization,
 * block/unblock controls, moderation reporting, and retention compliance.
 */
@Service
public class MessagingServiceImpl implements MessagingService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserAccountRepository userAccountRepository;
    private final CourseRepository courseRepository;
    private final UserRoleRepository userRoleRepository;
    private final FlaggedContentRepository flaggedContentRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final OwnershipService ownershipService;

    public MessagingServiceImpl(ConversationRepository conversationRepository,
                                MessageRepository messageRepository,
                                UserAccountRepository userAccountRepository,
                                CourseRepository courseRepository,
                                UserRoleRepository userRoleRepository,
                                FlaggedContentRepository flaggedContentRepository,
                                SimpMessagingTemplate messagingTemplate,
                                OwnershipService ownershipService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userAccountRepository = userAccountRepository;
        this.courseRepository = courseRepository;
        this.userRoleRepository = userRoleRepository;
        this.flaggedContentRepository = flaggedContentRepository;
        this.messagingTemplate = messagingTemplate;
        this.ownershipService = ownershipService;
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
        Long userId = principal.getUser() != null ? principal.getUser().getUserId() : null;

        if (userId != null) {
            List<Message> unread = messageRepository.findByConversation_IdAndReadAtIsNullAndSender_UserIdNot(conversationId, userId);
            LocalDateTime now = LocalDateTime.now();
            for (Message m : unread) {
                m.setReadAt(now);
            }
            if (!unread.isEmpty()) {
                messageRepository.saveAll(unread);
            }
        }

        return messageRepository.findByConversation_IdOrderBySentAtAsc(conversationId).stream()
                .map(MessageResponseDto::from)
                .toList();
    }

    @Override
    public Page<MessageResponseDto> getMessagesPaginated(Long conversationId, int page, int size, CustomUserDetails principal) {
        Conversation conversation = requireParticipant(conversationId, principal);
        Long userId = principal.getUser() != null ? principal.getUser().getUserId() : null;

        if (userId != null) {
            List<Message> unread = messageRepository.findByConversation_IdAndReadAtIsNullAndSender_UserIdNot(conversationId, userId);
            LocalDateTime now = LocalDateTime.now();
            for (Message m : unread) {
                m.setReadAt(now);
            }
            if (!unread.isEmpty()) {
                messageRepository.saveAll(unread);
            }
        }

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)), Sort.by(Sort.Direction.ASC, "sentAt"));
        Page<Message> messagePage = messageRepository.findByConversation_IdOrderBySentAtAsc(conversationId, pageable);
        return messagePage.map(MessageResponseDto::from);
    }

    @Override
    @Transactional
    public MessageResponseDto sendMessage(Long conversationId, SendMessageRequestDto request, CustomUserDetails principal) {
        Conversation conversation = requireParticipant(conversationId, principal);
        if (Boolean.TRUE.equals(conversation.getIsBlocked())) {
            throw new BadRequestException("This conversation is blocked.");
        }
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

        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, dto);

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

    @Override
    @Transactional
    public ConversationResponseDto blockConversation(Long conversationId, CustomUserDetails principal) {
        Conversation conversation = requireParticipant(conversationId, principal);
        conversation.setIsBlocked(true);
        conversation.setBlockedByUser(principal.getUser());
        Conversation saved = conversationRepository.save(conversation);
        Long currentUserId = principal.getUser().getUserId();
        long unread = messageRepository.countByConversation_IdAndReadAtIsNullAndSender_UserIdNot(conversationId, currentUserId);
        return ConversationResponseDto.from(saved, currentUserId, unread);
    }

    @Override
    @Transactional
    public ConversationResponseDto unblockConversation(Long conversationId, CustomUserDetails principal) {
        Conversation conversation = requireParticipant(conversationId, principal);
        boolean isAdmin = ownershipService.isAdmin(principal);
        boolean isBlocker = conversation.getBlockedByUser() != null
                && principal.getUser() != null
                && conversation.getBlockedByUser().getUserId().equals(principal.getUser().getUserId());
        if (!isAdmin && !isBlocker) {
            throw new BadRequestException("Only the blocker can unblock this conversation.");
        }
        conversation.setIsBlocked(false);
        conversation.setBlockedByUser(null);
        Conversation saved = conversationRepository.save(conversation);
        Long currentUserId = principal.getUser().getUserId();
        long unread = messageRepository.countByConversation_IdAndReadAtIsNullAndSender_UserIdNot(conversationId, currentUserId);
        return ConversationResponseDto.from(saved, currentUserId, unread);
    }

    @Override
    @Transactional
    public void reportConversation(Long conversationId, String reason, CustomUserDetails principal) {
        Conversation conversation = requireParticipant(conversationId, principal);
        FlaggedContent.Reason flagReason;
        try {
            flagReason = reason != null ? FlaggedContent.Reason.valueOf(reason.toUpperCase()) : FlaggedContent.Reason.SPAM;
        } catch (Exception e) {
            flagReason = FlaggedContent.Reason.SPAM;
        }

        List<Message> messages = messageRepository.findByConversation_IdOrderBySentAtAsc(conversationId);
        Long contentId = messages.isEmpty() ? conversationId : messages.get(messages.size() - 1).getId();

        FlaggedContent flag = new FlaggedContent();
        flag.setContentType(FlaggedContent.ContentType.MESSAGE);
        flag.setContentId(contentId);
        flag.setReason(flagReason);
        flag.setReporterId(principal.getUser().getUserId());
        flag.setStatus(FlaggedContent.Status.PENDING);
        flaggedContentRepository.save(flag);
    }

    private Conversation requireParticipant(Long conversationId, CustomUserDetails principal) {
        if (conversationId == null) {
            throw new ResourceNotFoundException("Conversation not found with id: null");
        }
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));
        if (ownershipService.isAdmin(principal)) {
            return conversation;
        }
        Long userId = principal != null && principal.getUser() != null ? principal.getUser().getUserId() : null;
        if (userId == null) {
            throw new ResourceNotFoundException("Conversation not found with id: " + conversationId);
        }
        boolean isParticipant = (conversation.getParticipantA() != null && conversation.getParticipantA().getUserId().equals(userId))
                || (conversation.getParticipantB() != null && conversation.getParticipantB().getUserId().equals(userId));
        if (!isParticipant) {
            throw new ResourceNotFoundException("Conversation not found with id: " + conversationId);
        }
        return conversation;
    }
}
