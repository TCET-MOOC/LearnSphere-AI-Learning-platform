package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.Feedback;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.FeedbackRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserAccountRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Security.OwnershipService;
import com.MOOC.OnlineLearningPlatfrom.Service.FeedbackService;
import com.MOOC.OnlineLearningPlatfrom.Service.PublicCacheService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Story W5-09: Feedback service enforcing enrolment eligibility, rating bounds (1-5),
 * exactly 1 review per course per user, author/admin deletion, and cached average rating.
 */
@Service
@Transactional
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final CourseRepository courseRepository;
    private final UserAccountRepository userAccountRepository;
    private final OwnershipService ownershipService;
    private final PublicCacheService publicCacheService;

    public FeedbackServiceImpl(FeedbackRepository feedbackRepository,
                               CourseRepository courseRepository,
                               UserAccountRepository userAccountRepository,
                               OwnershipService ownershipService,
                               PublicCacheService publicCacheService) {
        this.feedbackRepository = feedbackRepository;
        this.courseRepository = courseRepository;
        this.userAccountRepository = userAccountRepository;
        this.ownershipService = ownershipService;
        this.publicCacheService = publicCacheService;
    }

    @Override
    public Feedback submitFeedback(Long courseId, Short rating, String message, CustomUserDetails principal) {
        if (principal == null || principal.getUser() == null) {
            throw new ResourceNotFoundException("Authentication required");
        }
        if (rating == null || rating < 1 || rating > 5) {
            throw new BadRequestException("rating must be between 1 and 5 (inclusive)");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        ownershipService.requireEnrolled(courseId, principal);

        UserAccount user = principal.getUser();
        Optional<Feedback> existingOpt = feedbackRepository.findByUser_UserIdAndCourse_Id(user.getUserId(), courseId);
        Feedback target = existingOpt.orElseGet(Feedback::new);
        target.setCourse(course);
        target.setUser(user);
        target.setRating(rating);
        target.setMessage(message);

        Feedback saved = feedbackRepository.save(target);
        publicCacheService.invalidateCourseFeedbackAvg(courseId);
        return saved;
    }

    @Override
    public Feedback submitFeedback(Long courseId, Long userId, Short rating, String message) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new BadRequestException("rating must be between 1 and 5 (inclusive)");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Optional<Feedback> existingOpt = feedbackRepository.findByUser_UserIdAndCourse_Id(userId, courseId);
        Feedback target = existingOpt.orElseGet(Feedback::new);
        target.setCourse(course);
        target.setUser(user);
        target.setRating(rating);
        target.setMessage(message);

        Feedback saved = feedbackRepository.save(target);
        publicCacheService.invalidateCourseFeedbackAvg(courseId);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Feedback> getFeedbackForCourse(Long courseId) {
        courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
        return feedbackRepository.findByCourse_IdOrderByCreatedAtDesc(courseId);
    }

    @Override
    @Transactional(readOnly = true)
    public Feedback getFeedbackById(Long feedbackId) {
        return feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found with id: " + feedbackId));
    }

    @Override
    public void deleteFeedback(Long feedbackId, CustomUserDetails principal) {
        Feedback feedback = getFeedbackById(feedbackId);
        boolean isAdmin = ownershipService.isAdmin(principal);
        boolean isAuthor = feedback.getUser() != null && principal != null && principal.getUser() != null
                && feedback.getUser().getUserId().equals(principal.getUser().getUserId());

        if (!isAdmin && !isAuthor) {
            throw new ResourceNotFoundException("Feedback not found with id: " + feedbackId);
        }

        Long courseId = feedback.getCourse() != null ? feedback.getCourse().getId() : null;
        feedbackRepository.delete(feedback);
        if (courseId != null) {
            publicCacheService.invalidateCourseFeedbackAvg(courseId);
        }
    }

    @Override
    public void deleteFeedback(Long feedbackId, Long userId, boolean isAdmin) {
        Feedback feedback = getFeedbackById(feedbackId);
        if (!isAdmin && (feedback.getUser() == null || !feedback.getUser().getUserId().equals(userId))) {
            throw new ResourceNotFoundException("Feedback not found with id: " + feedbackId);
        }
        Long courseId = feedback.getCourse() != null ? feedback.getCourse().getId() : null;
        feedbackRepository.delete(feedback);
        if (courseId != null) {
            publicCacheService.invalidateCourseFeedbackAvg(courseId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAverageRating(Long courseId) {
        return getAverageRatingWithEtag(courseId).data();
    }

    @Override
    @Transactional(readOnly = true)
    public PublicCacheService.CacheResult<Map<String, Object>> getAverageRatingWithEtag(Long courseId) {
        courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        return publicCacheService.getCourseFeedbackAvg(courseId, () -> {
            Double avg = feedbackRepository.getAverageRatingForCourse(courseId);
            Long count = feedbackRepository.countByCourseId(courseId);
            double safeAvg = avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;

            return Map.of(
                    "courseId", courseId,
                    "averageRating", safeAvg,
                    "totalReviews", count != null ? count : 0L
            );
        });
    }
}
