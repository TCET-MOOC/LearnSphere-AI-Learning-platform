package com.MOOC.OnlineLearningPlatfrom.Service;

import com.MOOC.OnlineLearningPlatfrom.Dto.FlaggedContentDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.FlaggedContent;

import java.util.List;

public interface ModerationService {
    /** Scans existing content for concerning keywords, auto-creates flags for new hits,
     *  then returns the current pending queue (optionally filtered by category). */
    List<FlaggedContentDto> getFlagged(String category);

    FlaggedContentDto resolve(Long id);
    FlaggedContentDto dismiss(Long id);

    FlaggedContent.Reason classify(String text);
    void recordAutoFlag(FlaggedContent.ContentType type, Long contentId, FlaggedContent.Reason reason);

    FlaggedContentDto flagPost(Long postId, Long reporterId, String reason);
    FlaggedContentDto flagThread(Long threadId, Long reporterId, String reason);
}
