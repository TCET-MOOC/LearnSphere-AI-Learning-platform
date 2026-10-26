package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Entity.Feedback;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.FeedbackService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping("/courses/{courseId}/feedback")
    public ResponseEntity<Feedback> submitFeedback(
            @PathVariable Long courseId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Long userId = principal != null && principal.getUser() != null ? principal.getUser().getUserId() : 1L;
        Short rating = Short.valueOf(body.get("rating").toString());
        String message = body.get("message") != null ? body.get("message").toString() : null;

        return ResponseEntity.ok(feedbackService.submitFeedback(courseId, userId, rating, message));
    }

    @GetMapping("/courses/{courseId}/feedback")
    public ResponseEntity<List<Feedback>> getCourseFeedback(@PathVariable Long courseId) {
        return ResponseEntity.ok(feedbackService.getFeedbackForCourse(courseId));
    }

    @GetMapping("/courses/{courseId}/feedback/avg")
    public ResponseEntity<Map<String, Object>> getAverageRating(@PathVariable Long courseId) {
        return ResponseEntity.ok(feedbackService.getAverageRating(courseId));
    }

    @GetMapping("/feedback/{id}")
    public ResponseEntity<Feedback> getFeedbackById(@PathVariable Long id) {
        return ResponseEntity.ok(feedbackService.getFeedbackById(id));
    }

    @DeleteMapping("/feedback/{id}")
    public ResponseEntity<Void> deleteFeedback(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Long userId = principal != null && principal.getUser() != null ? principal.getUser().getUserId() : 0L;
        boolean isAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        feedbackService.deleteFeedback(id, userId, isAdmin);
        return ResponseEntity.noContent().build();
    }
}
