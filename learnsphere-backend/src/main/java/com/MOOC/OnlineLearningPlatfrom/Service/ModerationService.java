package com.MOOC.OnlineLearningPlatfrom.Service;

import com.MOOC.OnlineLearningPlatfrom.Dto.FlaggedContentDto;

import java.util.List;

public interface ModerationService {
    /** Scans existing content for concerning keywords, auto-creates flags for new hits,
     *  then returns the current pending queue (optionally filtered by category). */
    List<FlaggedContentDto> getFlagged(String category);

    FlaggedContentDto resolve(Long id);
    FlaggedContentDto dismiss(Long id);
}
