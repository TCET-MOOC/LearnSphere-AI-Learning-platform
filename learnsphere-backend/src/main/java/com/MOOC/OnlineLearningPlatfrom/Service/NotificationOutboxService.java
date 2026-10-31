package com.MOOC.OnlineLearningPlatfrom.Service;

import com.MOOC.OnlineLearningPlatfrom.Entity.NotificationEventType;
import com.MOOC.OnlineLearningPlatfrom.Entity.NotificationOutbox;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface NotificationOutboxService {

    NotificationOutbox recordEvent(NotificationEventType eventType,
                                   String aggregateType,
                                   String aggregateId,
                                   UserAccount recipient,
                                   String title,
                                   String body,
                                   Map<String, Object> payload);

    int processOutbox();

    void publishEnrolmentNotification(Long courseId, String courseTitle, UserAccount student);

    void publishGradeNotification(Long resourceId, String assessmentTitle, BigDecimal marks, UserAccount student);

    void publishFeedbackNotification(Long courseId, String courseTitle, Short rating, UserAccount instructor);

    void publishThreadReplyNotification(Long threadId, Long courseId, String threadTitle, String replyAuthorName, UserAccount recipient);

    void publishLiveStartNotification(Long sessionId, Long courseId, String sessionTitle, List<UserAccount> enrolledStudents);

    void publishAnnouncementNotification(Long announcementId, String announcementTitle, String body, List<UserAccount> recipients);
}

