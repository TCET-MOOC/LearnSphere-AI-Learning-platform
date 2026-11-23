package com.MOOC.OnlineLearningPlatfrom.Repository;

import com.MOOC.OnlineLearningPlatfrom.Entity.NotificationEventType;
import com.MOOC.OnlineLearningPlatfrom.Entity.NotificationOutbox;
import com.MOOC.OnlineLearningPlatfrom.Entity.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {

    @Query("SELECT o FROM NotificationOutbox o LEFT JOIN FETCH o.recipient WHERE o.status = :status ORDER BY o.createdAt ASC")
    List<NotificationOutbox> findByStatusOrderByCreatedAtAsc(@Param("status") OutboxStatus status, Pageable pageable);

    /**
     * Claims up to {@code limit} PENDING rows for this transaction (Wave 7 gate fix): rows another
     * processor already holds are skipped, so two schedulers/instances never deliver a row twice.
     */
    @Query(value = "SELECT * FROM notification_outbox WHERE status = 'PENDING' ORDER BY created_at ASC, id ASC "
            + "LIMIT :limit FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<NotificationOutbox> claimPending(@Param("limit") int limit);

    @Query("SELECT o FROM NotificationOutbox o LEFT JOIN FETCH o.recipient WHERE o.status = :status ORDER BY o.createdAt ASC")
    List<NotificationOutbox> findByStatusOrderByCreatedAtAsc(@Param("status") OutboxStatus status);

    boolean existsByEventTypeAndAggregateIdAndRecipient_UserId(NotificationEventType eventType, String aggregateId, Long recipientId);

    Optional<NotificationOutbox> findByEventTypeAndAggregateIdAndRecipient_UserId(NotificationEventType eventType, String aggregateId, Long recipientId);

    long countByStatus(OutboxStatus status);
}

