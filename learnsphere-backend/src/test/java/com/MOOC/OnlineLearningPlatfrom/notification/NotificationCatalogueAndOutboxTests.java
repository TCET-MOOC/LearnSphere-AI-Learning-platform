package com.MOOC.OnlineLearningPlatfrom.notification;

import com.MOOC.OnlineLearningPlatfrom.Entity.Notification;
import com.MOOC.OnlineLearningPlatfrom.Entity.NotificationEventType;
import com.MOOC.OnlineLearningPlatfrom.Entity.NotificationOutbox;
import com.MOOC.OnlineLearningPlatfrom.Entity.OutboxStatus;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Repository.NotificationOutboxRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.NotificationRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserAccountRepository;
import com.MOOC.OnlineLearningPlatfrom.Service.NotificationOutboxService;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Story W5-06: Notification event catalogue + outbox
 * Rules: A-17, [E-F14], NFR-03
 *
 * Scenarios:
 * 1. Fixed catalogue records outbox events: ENROLMENT, GRADE, FEEDBACK, THREAD_REPLY, LIVE_START, ANNOUNCEMENT.
 * 2. Outbox idempotency: Duplicate event on (event_type, aggregate_id, recipient_id) is suppressed.
 * 3. Outbox processor processes pending items, creates Notification rows, delivers via WebSocket, and marks PROCESSED.
 * 4. Outbox processor retry/failure handling: Error increments retry_count; marks FAILED on max retries.
 * 5. GET /api/notifications returns user-scoped notifications with Cache-Control: private, no-store.
 * 6. Mark read ownership: user can mark own notification read; foreign notification returns 403 Forbidden.
 * 7. Mark all read and unread count: accurately reflects read state.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class NotificationCatalogueAndOutboxTests extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationOutboxService outboxService;

    @Autowired
    private NotificationOutboxRepository outboxRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private UserDetailsService userDetailsService;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    private UserAccount student;
    private UserAccount teacher;
    private UserAccount foreignStudent;

    @BeforeEach
    public void setup() {
        notificationRepository.deleteAll();
        outboxRepository.deleteAll();

        student = userAccountRepository.findByEmail("student@learnsphere.com");
        teacher = userAccountRepository.findByEmail("teacher@learnsphere.com");
        foreignStudent = userAccountRepository.findByEmail("external@learnsphere.com");
    }

    private RequestPostProcessor auth(UserAccount u) {
        return user(userDetailsService.loadUserByUsername(u.getEmail()));
    }

    @Test
    @DisplayName("Scenario 1: Fixed catalogue records outbox events for all 6 event types")
    public void testRecordAllCatalogueEvents() {
        // 1. ENROLMENT
        outboxService.publishEnrolmentNotification(101L, "Cloud Architecture", student);
        // 2. GRADE
        outboxService.publishGradeNotification(201L, "Midterm Exam", new BigDecimal("88.50"), student);
        // 3. FEEDBACK
        outboxService.publishFeedbackNotification(101L, "Cloud Architecture", (short) 5, teacher);
        // 4. THREAD_REPLY
        outboxService.publishThreadReplyNotification(301L, 101L, "Need help with Lab 2", "Bob Student", student);
        // 5. LIVE_START
        outboxService.publishLiveStartNotification(401L, 101L, "Live Q&A Session", List.of(student));
        // 6. ANNOUNCEMENT
        outboxService.publishAnnouncementNotification(501L, "Exam Schedule Released", "The exam starts on Dec 1st", List.of(student));

        List<NotificationOutbox> pending = outboxRepository.findAll();
        assertThat(pending).hasSize(6);
        assertThat(pending).extracting(NotificationOutbox::getEventType)
                .containsExactlyInAnyOrder(
                        NotificationEventType.ENROLMENT,
                        NotificationEventType.GRADE,
                        NotificationEventType.FEEDBACK,
                        NotificationEventType.THREAD_REPLY,
                        NotificationEventType.LIVE_START,
                        NotificationEventType.ANNOUNCEMENT
                );
        assertThat(pending).allMatch(o -> o.getStatus() == OutboxStatus.PENDING);
    }

    @Test
    @DisplayName("Scenario 2: Outbox idempotency — duplicate (eventType, aggregateId, recipientId) is suppressed")
    public void testOutboxIdempotency() {
        outboxService.publishEnrolmentNotification(101L, "Cloud Architecture", student);
        // Duplicate publish for same course and student
        outboxService.publishEnrolmentNotification(101L, "Cloud Architecture", student);

        List<NotificationOutbox> items = outboxRepository.findAll();
        assertThat(items).hasSize(1);
    }

    @Test
    @DisplayName("Scenario 3: Outbox processor creates Notification, delivers via WebSocket, and marks PROCESSED")
    public void testOutboxProcessorTransformsToNotifications() {
        outboxService.publishEnrolmentNotification(101L, "Cloud Architecture", student);
        outboxService.publishGradeNotification(201L, "Midterm Exam", new BigDecimal("95.00"), student);

        int processedCount = outboxService.processOutbox();
        assertThat(processedCount).isEqualTo(2);

        // Verify notifications created
        List<Notification> notifications = notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(student.getUserId());
        assertThat(notifications).hasSize(2);
        assertThat(notifications).extracting(Notification::getCategory)
                .containsExactlyInAnyOrder("ENROLMENT", "GRADE");

        // Verify outbox status updated to PROCESSED with timestamp
        List<NotificationOutbox> outboxItems = outboxRepository.findAll();
        assertThat(outboxItems).allMatch(o -> o.getStatus() == OutboxStatus.PROCESSED);
        assertThat(outboxItems).allMatch(o -> o.getProcessedAt() != null);

        // Verify WebSocket delivery was invoked
        verify(messagingTemplate, atLeastOnce()).convertAndSend(anyString(), (Object) any());
    }

    @Test
    @DisplayName("Scenario 4: Outbox processor handles failure and increments retry count")
    public void testOutboxProcessorFailureHandling() {
        doThrow(new RuntimeException("Simulated broker transport failure"))
                .when(messagingTemplate).convertAndSend(anyString(), (Object) any());

        outboxService.publishEnrolmentNotification(101L, "Cloud Architecture", student);

        outboxService.processOutbox();

        NotificationOutbox item = outboxRepository.findAll().get(0);
        assertThat(item.getRetryCount()).isEqualTo(1);
        assertThat(item.getErrorMessage()).contains("Simulated broker transport failure");
        // Remains PENDING until max retries reached
        assertThat(item.getStatus()).isEqualTo(OutboxStatus.PENDING);
    }

    @Test
    @DisplayName("Scenario 5: GET /api/notifications returns user-scoped notifications with Cache-Control: private, no-store")
    public void testNotificationsListEndpointAndCacheControl() throws Exception {
        outboxService.publishEnrolmentNotification(101L, "Cloud Architecture", student);
        outboxService.publishEnrolmentNotification(102L, "Advanced Algorithms", foreignStudent);
        outboxService.processOutbox();

        mockMvc.perform(get("/api/notifications")
                        .with(auth(student)))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", containsString("Cloud Architecture")));
    }

    @Test
    @DisplayName("Scenario 6: Mark read ownership — user can mark own notification read, but foreign returns 403")
    public void testMarkReadOwnershipAuthorization() throws Exception {
        outboxService.publishEnrolmentNotification(101L, "Cloud Architecture", student);
        outboxService.processOutbox();

        Notification n = notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(student.getUserId()).get(0);

        // Foreign student tries to mark student's notification read -> 403 Forbidden
        mockMvc.perform(put("/api/notifications/{id}/read", n.getId())
                        .with(auth(foreignStudent)))
                .andExpect(status().isForbidden());

        // Student marks own notification read -> 200 OK
        mockMvc.perform(put("/api/notifications/{id}/read", n.getId())
                        .with(auth(student)))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.read").value(true));
    }

    @Test
    @DisplayName("Scenario 7: Mark all read and unread count update state accurately")
    public void testMarkAllReadAndUnreadCount() throws Exception {
        outboxService.publishEnrolmentNotification(101L, "Cloud Architecture", student);
        outboxService.publishGradeNotification(201L, "Midterm Exam", new BigDecimal("90.00"), student);
        outboxService.processOutbox();

        // Check unread count is 2
        mockMvc.perform(get("/api/notifications/unread-count")
                        .with(auth(student)))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.count").value(2));

        // Mark all read
        mockMvc.perform(put("/api/notifications/read-all")
                        .with(auth(student)))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Cache-Control", containsString("no-store")));

        // Check unread count is now 0
        mockMvc.perform(get("/api/notifications/unread-count")
                        .with(auth(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }
}

