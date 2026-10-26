package com.MOOC.OnlineLearningPlatfrom.Repository;

import com.MOOC.OnlineLearningPlatfrom.Entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByThread_ThreadIdOrderByCreatedAtAsc(Long threadId);
    List<Post> findByThread_ThreadIdAndParentPostIsNullOrderByCreatedAtAsc(Long threadId);
}
