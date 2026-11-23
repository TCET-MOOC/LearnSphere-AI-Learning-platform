package com.MOOC.OnlineLearningPlatfrom.notification;

import com.MOOC.OnlineLearningPlatfrom.Entity.OutboxStatus;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Repository.NotificationOutboxRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.NotificationRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserAccountRepository;
import com.MOOC.OnlineLearningPlatfrom.Service.NotificationOutboxService;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Wave 7 gate stop-the-line fix (found by W7-16 and the gate run; A-17, W5-06): an outbox row is
 * delivered exactly once even when two processors run at the same time (the 5 s scheduler and a
 * direct call, or two instances), and tests drive the outbox themselves — the scheduler is off in
 * the test profile.
 */
@SpringBootTest
@ActiveProfiles("test")
class NotificationOutboxConcurrencyTests extends PostgresIntegrationTest {

    private static final int ROWS = 40;

    @Autowired private NotificationOutboxService outboxService;
    @Autowired private NotificationOutboxRepository outboxRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private ApplicationContext context;

    @MockBean private SimpMessagingTemplate messagingTemplate;

    private UserAccount student;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        outboxRepository.deleteAll();
        student = userAccountRepository.findByEmail("student@learnsphere.com");
        for (int i = 0; i < ROWS; i++) {
            outboxService.publishGradeNotification(9_000L + i, "Quiz " + i, new BigDecimal("5.00"), student);
        }
    }

    @Test
    void twoConcurrentProcessorsDeliverEachRowExactlyOnce() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> runs = List.of(
                    pool.submit(() -> { start.await(); return outboxService.processOutbox(); }),
                    pool.submit(() -> { start.await(); return outboxService.processOutbox(); }));
            start.countDown();
            int processed = 0;
            for (Future<Integer> run : runs) processed += run.get(60, TimeUnit.SECONDS);

            assertThat(processed).as("rows processed across both runs").isEqualTo(ROWS);
        } finally {
            pool.shutdownNow();
        }
        assertThat(notificationRepository.count()).as("one notification per outbox row").isEqualTo(ROWS);
        assertThat(outboxRepository.findAll()).allMatch(o -> o.getStatus() == OutboxStatus.PROCESSED);
    }

    @Test
    void theScheduledProcessorIsOffInTheTestProfile() {
        assertThat(context.getBeanNamesForType(
                        com.MOOC.OnlineLearningPlatfrom.Service.impl.NotificationOutboxScheduler.class))
                .as("tests call processOutbox() themselves; no background run may race them")
                .isEmpty();
    }
}
