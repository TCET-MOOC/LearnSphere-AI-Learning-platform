package com.MOOC.OnlineLearningPlatfrom.Repository;

import com.MOOC.OnlineLearningPlatfrom.Entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByConversation_IdOrderBySentAtAsc(Long conversationId);

    long countByConversation_IdAndReadAtIsNullAndSender_UserIdNot(Long conversationId, Long senderId);

    List<Message> findByConversation_IdAndReadAtIsNullAndSender_UserIdNot(Long conversationId, Long senderId);
}
