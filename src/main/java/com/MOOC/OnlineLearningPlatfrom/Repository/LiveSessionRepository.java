package com.MOOC.OnlineLearningPlatfrom.Repository;

import com.MOOC.OnlineLearningPlatfrom.Entity.LiveSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LiveSessionRepository extends JpaRepository<LiveSession, Long> {
    List<LiveSession> findByTeacher_UserIdOrderByScheduledAtDesc(Long teacherId);
}
