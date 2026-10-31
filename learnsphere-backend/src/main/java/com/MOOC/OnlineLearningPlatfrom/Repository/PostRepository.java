package com.MOOC.OnlineLearningPlatfrom.Repository;

import com.MOOC.OnlineLearningPlatfrom.Entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.user WHERE p.thread.threadId = :threadId ORDER BY p.createdAt ASC")
    List<Post> findByThread_ThreadIdOrderByCreatedAtAsc(@Param("threadId") Long threadId);

    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.user WHERE p.thread.threadId = :threadId AND p.parentPost IS NULL ORDER BY p.createdAt ASC")
    List<Post> findByThread_ThreadIdAndParentPostIsNullOrderByCreatedAtAsc(@Param("threadId") Long threadId);

    @Modifying
    void deleteByThread_ThreadId(Long threadId);

    long countByThread_ThreadId(Long threadId);
}
