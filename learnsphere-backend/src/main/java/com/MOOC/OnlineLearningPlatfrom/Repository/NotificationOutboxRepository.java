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

    @Query("SELECT o FROM NotificationOutbox o LEFT JOIN FETCH o.recipient WHERE o.status = :status ORDER BY o.createdAt ASC")
    List<NotificationOutbox> findByStatusOrderByCreatedAtAsc(@Param("status") OutboxStatus status);

    boolean existsByEventTypeAndAggregateIdAndRecipient_UserId(NotificationEventType eventType, String aggregateId, Long recipientId);

    Optional<NotificationOutbox> findByEventTypeAndAggregateIdAndRecipient_UserId(NotificationEventType eventType, String aggregateId, Long recipientId);

    long countByStatus(OutboxStatus status);
}

