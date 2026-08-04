package com.MOOC.OnlineLearningPlatfrom.Repository;

import com.MOOC.OnlineLearningPlatfrom.Entity.FlaggedContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlaggedContentRepository extends JpaRepository<FlaggedContent, Long> {
    boolean existsByContentTypeAndContentId(FlaggedContent.ContentType contentType, Long contentId);
    List<FlaggedContent> findByStatusOrderByCreatedAtDesc(FlaggedContent.Status status);
    List<FlaggedContent> findByStatusAndReasonOrderByCreatedAtDesc(FlaggedContent.Status status, FlaggedContent.Reason reason);
    long countByStatus(FlaggedContent.Status status);
}
