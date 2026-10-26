package com.MOOC.OnlineLearningPlatfrom.Repository;

import com.MOOC.OnlineLearningPlatfrom.Entity.DiscussionThread;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiscussionThreadRepository extends JpaRepository<DiscussionThread, Long> {
    List<DiscussionThread> findByCourse_IdOrderByCreatedAtDesc(Long courseId);
}
