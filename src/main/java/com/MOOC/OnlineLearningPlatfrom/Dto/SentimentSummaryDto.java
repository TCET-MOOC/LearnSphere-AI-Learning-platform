package com.MOOC.OnlineLearningPlatfrom.Dto;

import java.util.List;

/**
 * Platform-wide sentiment snapshot computed with a simple keyword-based heuristic
 * (see Service/support/KeywordLists.java) over DiscussionPost.body + Message.text.
 * This is NOT real NLP/ML sentiment analysis.
 */
public class SentimentSummaryDto {
    private long totalAnalyzed;
    private int positivePercent;
    private int neutralPercent;
    private int negativePercent;
    private List<CourseTypeSentimentDto> courseTypeSentiment;
    private List<NegativeKeywordDto> negativeKeywords;
    private List<RecommendationDto> recommendations;
    private List<TeacherScoreDto> teacherScores;

    public SentimentSummaryDto(long totalAnalyzed, int positivePercent, int neutralPercent, int negativePercent,
                                List<CourseTypeSentimentDto> courseTypeSentiment, List<NegativeKeywordDto> negativeKeywords,
                                List<RecommendationDto> recommendations, List<TeacherScoreDto> teacherScores) {
        this.totalAnalyzed = totalAnalyzed;
        this.positivePercent = positivePercent;
        this.neutralPercent = neutralPercent;
        this.negativePercent = negativePercent;
        this.courseTypeSentiment = courseTypeSentiment;
        this.negativeKeywords = negativeKeywords;
        this.recommendations = recommendations;
        this.teacherScores = teacherScores;
    }

    public long getTotalAnalyzed() { return totalAnalyzed; }
    public int getPositivePercent() { return positivePercent; }
    public int getNeutralPercent() { return neutralPercent; }
    public int getNegativePercent() { return negativePercent; }
    public List<CourseTypeSentimentDto> getCourseTypeSentiment() { return courseTypeSentiment; }
    public List<NegativeKeywordDto> getNegativeKeywords() { return negativeKeywords; }
    public List<RecommendationDto> getRecommendations() { return recommendations; }
    public List<TeacherScoreDto> getTeacherScores() { return teacherScores; }
}
