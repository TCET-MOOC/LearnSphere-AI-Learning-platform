package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Service.NotificationOutboxService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs the notification outbox every 5 s (W5-06; split out of the service at the Wave 7 gate).
 * On unless {@code app.notifications.outbox.scheduler.enabled=false}; the test profile turns it off
 * so tests drive {@code processOutbox()} themselves instead of racing a background run.
 */
@Component
@ConditionalOnProperty(name = "app.notifications.outbox.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class NotificationOutboxScheduler {

    private final NotificationOutboxService outboxService;

    public NotificationOutboxScheduler(NotificationOutboxService outboxService) {
        this.outboxService = outboxService;
    }

    @Scheduled(fixedDelay = 5000)
    public void run() {
        outboxService.processOutbox();
    }
}
