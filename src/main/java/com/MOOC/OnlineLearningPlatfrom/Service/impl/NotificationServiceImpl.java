package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Dto.NotificationResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.Notification;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.NotificationRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.NotificationService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public List<NotificationResponseDto> getNotifications(CustomUserDetails principal) {
        return notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(principal.getUser().getUserId()).stream()
                .map(NotificationResponseDto::from)
                .toList();
    }

    @Override
    public NotificationResponseDto markRead(Long id, CustomUserDetails principal) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));
        if (notification.getUser() == null || !notification.getUser().getUserId().equals(principal.getUser().getUserId())) {
            throw new AccessDeniedException("You cannot modify another user's notification");
        }
        notification.setRead(true);
        return NotificationResponseDto.from(notificationRepository.save(notification));
    }

    @Override
    public void markAllRead(CustomUserDetails principal) {
        List<Notification> unread = notificationRepository.findByUser_UserIdAndReadFalse(principal.getUser().getUserId());
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    @Override
    public long getUnreadCount(CustomUserDetails principal) {
        return notificationRepository.countByUser_UserIdAndReadFalse(principal.getUser().getUserId());
    }

    @Override
    public NotificationResponseDto create(UserAccount user, String title, String body, String category) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setCategory(category);
        notification.setRead(false);
        return NotificationResponseDto.from(notificationRepository.save(notification));
    }
}
