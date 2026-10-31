package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Entity.Feedback;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.FeedbackService;
import com.MOOC.OnlineLearningPlatfrom.Service.PublicCacheService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for course feedback and reviews (W5-09).
 * Enforces enrolment gating, rating bounds (1-5), single review per course,
 * author/admin deletion, and cached public average rating with ETag.
 */
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
        if (principal == null || principal.getUser() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (body == null || body.get("rating") == null) {
            throw new BadRequestException("rating is required");
        }
        Short rating = Short.valueOf(body.get("rating").toString());
        String message = body.get("message") != null ? body.get("message").toString() : null;

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(feedbackService.submitFeedback(courseId, rating, message, principal));
    }

    @GetMapping("/courses/{courseId}/feedback")
    public ResponseEntity<List<Feedback>> getCourseFeedback(@PathVariable Long courseId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(feedbackService.getFeedbackForCourse(courseId));
    }

    @GetMapping("/courses/{courseId}/feedback/avg")
    public ResponseEntity<Map<String, Object>> getAverageRating(
            @PathVariable Long courseId,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
        PublicCacheService.CacheResult<Map<String, Object>> result = feedbackService.getAverageRatingWithEtag(courseId);
        if (ifNoneMatch != null && ifNoneMatch.equals(result.etag())) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(result.etag())
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=60")
                    .build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=60")
                .eTag(result.etag())
                .body(result.data());
    }

    @GetMapping("/feedback/{id}")
    public ResponseEntity<Feedback> getFeedbackById(@PathVariable Long id) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(feedbackService.getFeedbackById(id));
    }

    @DeleteMapping("/feedback/{id}")
    public ResponseEntity<Void> deleteFeedback(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        if (principal == null || principal.getUser() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        feedbackService.deleteFeedback(id, principal);
        return ResponseEntity.noContent()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .build();
    }
}
