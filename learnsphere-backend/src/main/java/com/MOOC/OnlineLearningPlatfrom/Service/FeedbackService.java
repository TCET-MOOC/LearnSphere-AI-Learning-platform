package com.MOOC.OnlineLearningPlatfrom.Service;

import com.MOOC.OnlineLearningPlatfrom.Entity.Feedback;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;

import java.util.List;
import java.util.Map;

public interface FeedbackService {
    Feedback submitFeedback(Long courseId, Short rating, String message, CustomUserDetails principal);
    Feedback submitFeedback(Long courseId, Long userId, Short rating, String message);
    List<Feedback> getFeedbackForCourse(Long courseId);
    Feedback getFeedbackById(Long feedbackId);
    void deleteFeedback(Long feedbackId, CustomUserDetails principal);
    void deleteFeedback(Long feedbackId, Long userId, boolean isAdmin);
    Map<String, Object> getAverageRating(Long courseId);
    PublicCacheService.CacheResult<Map<String, Object>> getAverageRatingWithEtag(Long courseId);
}
