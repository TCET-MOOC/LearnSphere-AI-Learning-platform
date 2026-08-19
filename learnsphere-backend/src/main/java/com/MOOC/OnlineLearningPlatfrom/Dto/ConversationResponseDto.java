package com.MOOC.OnlineLearningPlatfrom.Dto;

import com.MOOC.OnlineLearningPlatfrom.Entity.Conversation;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;

import java.time.LocalDateTime;

public class ConversationResponseDto {
    private Long id;
    private Long otherUserId;
    private String otherUserName;
    private String otherUserAvatarUrl;
    private String otherUserRole;
    private Long courseId;
    private String courseName;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;
    private long unreadCount;

    public static ConversationResponseDto from(Conversation conversation, Long currentUserId, long unreadCount) {
        return from(conversation, currentUserId, unreadCount, null);
    }

    public static ConversationResponseDto from(Conversation conversation, Long currentUserId, long unreadCount, String otherUserRole) {
        ConversationResponseDto dto = new ConversationResponseDto();
        dto.id = conversation.getId();

        UserAccount other = conversation.getParticipantA() != null
                && conversation.getParticipantA().getUserId().equals(currentUserId)
                ? conversation.getParticipantB()
                : conversation.getParticipantA();

        if (other != null) {
            dto.otherUserId = other.getUserId();
            dto.otherUserName = other.getFullName();
            dto.otherUserAvatarUrl = other.getAvatarUrl();
        }
        dto.otherUserRole = otherUserRole;

        if (conversation.getCourse() != null) {
            dto.courseId = conversation.getCourse().getId();
            dto.courseName = conversation.getCourse().getTitle();
        }

        dto.lastMessage = conversation.getLastMessage();
        dto.lastMessageAt = conversation.getLastMessageAt();
        dto.createdAt = conversation.getCreatedAt();
        dto.unreadCount = unreadCount;
        return dto;
    }

    public Long getId() { return id; }
    public Long getOtherUserId() { return otherUserId; }
    public String getOtherUserName() { return otherUserName; }
    public String getOtherUserAvatarUrl() { return otherUserAvatarUrl; }
    public String getOtherUserRole() { return otherUserRole; }
    public Long getCourseId() { return courseId; }
    public String getCourseName() { return courseName; }
    public String getLastMessage() { return lastMessage; }
    public LocalDateTime getLastMessageAt() { return lastMessageAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public long getUnreadCount() { return unreadCount; }
}
