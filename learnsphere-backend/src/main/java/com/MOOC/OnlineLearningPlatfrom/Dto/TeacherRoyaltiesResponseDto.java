package com.MOOC.OnlineLearningPlatfrom.Dto;

import java.util.List;

/** Full response for GET /api/teacher/royalties. */
public class TeacherRoyaltiesResponseDto {
    private RoyaltySummaryDto summary;
    private List<RoyaltyBreakdownDto> byCourse;
    private List<RoyaltySourceSplitDto> sourceBreakdown;

    public TeacherRoyaltiesResponseDto(RoyaltySummaryDto summary, List<RoyaltyBreakdownDto> byCourse,
                                        List<RoyaltySourceSplitDto> sourceBreakdown) {
        this.summary = summary;
        this.byCourse = byCourse;
        this.sourceBreakdown = sourceBreakdown;
    }

    public RoyaltySummaryDto getSummary() { return summary; }
    public List<RoyaltyBreakdownDto> getByCourse() { return byCourse; }
    public List<RoyaltySourceSplitDto> getSourceBreakdown() { return sourceBreakdown; }
}
