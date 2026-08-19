package com.MOOC.OnlineLearningPlatfrom.Service;

import com.MOOC.OnlineLearningPlatfrom.Dto.NotificationResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;

import java.util.List;

public interface NotificationService {
    List<NotificationResponseDto> getNotifications(CustomUserDetails principal);
    NotificationResponseDto markRead(Long id, CustomUserDetails principal);
    void markAllRead(CustomUserDetails principal);
    long getUnreadCount(CustomUserDetails principal);

    /**
     * Generic helper other domains/workstreams can call to create a real, persisted
     * notification for a user (e.g. a teacher nudge, a graded assignment, a new message).
     */
    NotificationResponseDto create(UserAccount user, String title, String body, String category);
}
