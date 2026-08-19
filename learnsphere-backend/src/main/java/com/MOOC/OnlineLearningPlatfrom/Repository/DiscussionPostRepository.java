package com.MOOC.OnlineLearningPlatfrom.Repository;

import com.MOOC.OnlineLearningPlatfrom.Entity.DiscussionPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiscussionPostRepository extends JpaRepository<DiscussionPost, Long> {
    List<DiscussionPost> findByCourse_IdAndParentPostIsNullOrderByCreatedAtDesc(Long courseId);
    List<DiscussionPost> findByLecture_IdAndParentPostIsNullOrderByCreatedAtDesc(Long lectureId);
    List<DiscussionPost> findByParentPost_IdOrderByCreatedAtAsc(Long parentPostId);
}
