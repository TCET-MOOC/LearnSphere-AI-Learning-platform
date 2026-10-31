package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Dto.NotificationResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.Notification;
import com.MOOC.OnlineLearningPlatfrom.Entity.NotificationEventType;
import com.MOOC.OnlineLearningPlatfrom.Entity.NotificationOutbox;
import com.MOOC.OnlineLearningPlatfrom.Entity.OutboxStatus;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Repository.NotificationOutboxRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.NotificationRepository;
import com.MOOC.OnlineLearningPlatfrom.Service.NotificationOutboxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class NotificationOutboxServiceImpl implements NotificationOutboxService {

    private static final Logger log = LoggerFactory.getLogger(NotificationOutboxServiceImpl.class);
    private static final int MAX_RETRIES = 3;

    private final NotificationOutboxRepository outboxRepository;
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public NotificationOutboxServiceImpl(NotificationOutboxRepository outboxRepository,
                                         NotificationRepository notificationRepository,
                                         SimpMessagingTemplate messagingTemplate,
                                         ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public NotificationOutbox recordEvent(NotificationEventType eventType,
                                          String aggregateType,
                                          String aggregateId,
                                          UserAccount recipient,
                                          String title,
                                          String body,
                                          Map<String, Object> payload) {
        if (recipient == null || recipient.getUserId() == null) {
            log.warn("Cannot record notification outbox event: recipient is null");
            return null;
        }

        // Idempotency check: (event_type, aggregate_id, recipient_id)
        if (outboxRepository.existsByEventTypeAndAggregateIdAndRecipient_UserId(eventType, aggregateId, recipient.getUserId())) {
            log.debug("Outbox event already recorded for eventType={}, aggregateId={}, recipientId={}",
                    eventType, aggregateId, recipient.getUserId());
            return outboxRepository.findByEventTypeAndAggregateIdAndRecipient_UserId(eventType, aggregateId, recipient.getUserId())
                    .orElse(null);
        }

        NotificationOutbox outbox = new NotificationOutbox();
        outbox.setEventType(eventType);
        outbox.setAggregateType(aggregateType);
        outbox.setAggregateId(aggregateId);
        outbox.setRecipient(recipient);
        outbox.setTitle(title);
        outbox.setBody(body);
        outbox.setStatus(OutboxStatus.PENDING);
        outbox.setCreatedAt(LocalDateTime.now());
        outbox.setRetryCount(0);

        if (payload != null && !payload.isEmpty()) {
            try {
                outbox.setPayload(objectMapper.writeValueAsString(payload));
            } catch (Exception e) {
                log.warn("Failed to serialize notification payload: {}", e.getMessage());
                outbox.setPayload(payload.toString());
            }
        }

        return outboxRepository.save(outbox);
    }

    @Override
    @Transactional
    public int processOutbox() {
        List<NotificationOutbox> pending = outboxRepository.findByStatusOrderByCreatedAtAsc(
                OutboxStatus.PENDING, PageRequest.of(0, 100));

        int processed = 0;
        for (NotificationOutbox item : pending) {
            try {
                // 1. Create durable Notification entity
                Notification notification = new Notification();
                notification.setUser(item.getRecipient());
                notification.setTitle(item.getTitle());
                notification.setBody(item.getBody());
                notification.setCategory(item.getEventType().name());
                notification.setRead(false);
                Notification saved = notificationRepository.save(notification);

                // 2. Deliver real-time WebSocket notification to user
                NotificationResponseDto dto = NotificationResponseDto.from(saved);
                if (item.getRecipient() != null) {
                    if (item.getRecipient().getUserId() != null) {
                        messagingTemplate.convertAndSend("/topic/notifications/" + item.getRecipient().getUserId(), dto);
                    }
                    if (item.getRecipient().getEmail() != null) {
                        messagingTemplate.convertAndSendToUser(item.getRecipient().getEmail(), "/queue/notifications", dto);
                    }
                }

                // 3. Mark outbox entry as PROCESSED
                item.setStatus(OutboxStatus.PROCESSED);
                item.setProcessedAt(LocalDateTime.now());
                item.setErrorMessage(null);
                outboxRepository.save(item);
                processed++;
            } catch (Exception e) {
                log.error("Failed to process notification outbox item {}: {}", item.getId(), e.getMessage());
                int retries = (item.getRetryCount() != null ? item.getRetryCount() : 0) + 1;
                item.setRetryCount(retries);
                item.setErrorMessage(e.getMessage());
                if (retries >= MAX_RETRIES) {
                    item.setStatus(OutboxStatus.FAILED);
                }
                outboxRepository.save(item);
            }
        }
        return processed;
    }

    @Scheduled(fixedDelay = 5000)
    public void scheduledOutboxProcessor() {
        processOutbox();
    }

    @Override
    @Transactional
    public void publishEnrolmentNotification(Long courseId, String courseTitle, UserAccount student) {
        String title = "Enrolled in " + courseTitle;
        String body = "You have successfully enrolled in " + courseTitle + ". Start learning today!";
        recordEvent(NotificationEventType.ENROLMENT, "COURSE", String.valueOf(courseId), student, title, body,
                Map.of("courseId", courseId, "courseTitle", courseTitle));
    }

    @Override
    @Transactional
    public void publishGradeNotification(Long resourceId, String assessmentTitle, BigDecimal marks, UserAccount student) {
        String title = "New Grade Posted: " + assessmentTitle;
        String body = "Your assessment for \"" + assessmentTitle + "\" has been graded: " + marks + " marks.";
        recordEvent(NotificationEventType.GRADE, "ASSESSMENT", String.valueOf(resourceId), student, title, body,
                Map.of("resourceId", resourceId, "marks", marks));
    }

    @Override
    @Transactional
    public void publishFeedbackNotification(Long courseId, String courseTitle, Short rating, UserAccount instructor) {
        String title = "New Feedback: " + courseTitle;
        String body = "A student rated \"" + courseTitle + "\" " + rating + " / 5 stars.";
        recordEvent(NotificationEventType.FEEDBACK, "COURSE", String.valueOf(courseId) + ":" + instructor.getUserId(),
                instructor, title, body, Map.of("courseId", courseId, "rating", rating));
    }

    @Override
    @Transactional
    public void publishThreadReplyNotification(Long threadId, Long courseId, String threadTitle, String replyAuthorName, UserAccount recipient) {
        String title = "New Reply: " + threadTitle;
        String body = replyAuthorName + " replied to \"" + threadTitle + "\".";
        recordEvent(NotificationEventType.THREAD_REPLY, "THREAD", String.valueOf(threadId) + ":" + recipient.getUserId(),
                recipient, title, body, Map.of("threadId", threadId, "courseId", courseId));
    }

    @Override
    @Transactional
    public void publishLiveStartNotification(Long sessionId, Long courseId, String sessionTitle, List<UserAccount> enrolledStudents) {
        String title = "Live Session Started: " + sessionTitle;
        String body = "The live session \"" + sessionTitle + "\" is starting now. Click to join!";
        if (enrolledStudents != null) {
            for (UserAccount student : enrolledStudents) {
                recordEvent(NotificationEventType.LIVE_START, "LIVE_SESSION", String.valueOf(sessionId),
                        student, title, body, Map.of("sessionId", sessionId, "courseId", courseId));
            }
        }
    }

    @Override
    @Transactional
    public void publishAnnouncementNotification(Long announcementId, String announcementTitle, String body, List<UserAccount> recipients) {
        String title = "Announcement: " + announcementTitle;
        if (recipients != null) {
            for (UserAccount r : recipients) {
                recordEvent(NotificationEventType.ANNOUNCEMENT, "ANNOUNCEMENT", String.valueOf(announcementId),
                        r, title, body, Map.of("announcementId", announcementId));
            }
        }
    }
}

