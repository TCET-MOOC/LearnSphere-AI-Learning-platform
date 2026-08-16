package com.MOOC.OnlineLearningPlatfrom.Repository;

import com.MOOC.OnlineLearningPlatfrom.Entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("SELECT c FROM Conversation c WHERE c.participantA.userId = :userId OR c.participantB.userId = :userId " +
            "ORDER BY c.lastMessageAt DESC NULLS LAST, c.createdAt DESC")
    List<Conversation> findAllForUser(@Param("userId") Long userId);

    @Query("SELECT c FROM Conversation c WHERE " +
            "((c.participantA.userId = :userA AND c.participantB.userId = :userB) OR " +
            "(c.participantA.userId = :userB AND c.participantB.userId = :userA)) " +
            "AND ((:courseId IS NULL AND c.course IS NULL) OR c.course.id = :courseId)")
    Optional<Conversation> findExisting(@Param("userA") Long userA, @Param("userB") Long userB, @Param("courseId") Long courseId);
}
