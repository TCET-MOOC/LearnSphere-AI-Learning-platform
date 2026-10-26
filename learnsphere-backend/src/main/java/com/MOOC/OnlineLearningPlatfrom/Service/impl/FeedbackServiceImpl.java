package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.Feedback;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.FeedbackRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserAccountRepository;
import com.MOOC.OnlineLearningPlatfrom.Service.FeedbackService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final CourseRepository courseRepository;
    private final UserAccountRepository userAccountRepository;

    public FeedbackServiceImpl(FeedbackRepository feedbackRepository,
                               CourseRepository courseRepository,
                               UserAccountRepository userAccountRepository) {
        this.feedbackRepository = feedbackRepository;
        this.courseRepository = courseRepository;
        this.userAccountRepository = userAccountRepository;
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

        return feedbackRepository.save(target);
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
    public void deleteFeedback(Long feedbackId, Long userId, boolean isAdmin) {
        Feedback feedback = getFeedbackById(feedbackId);
        if (!isAdmin && (feedback.getUser() == null || !feedback.getUser().getUserId().equals(userId))) {
            throw new BadRequestException("You can only delete your own feedback");
        }
        feedbackRepository.delete(feedback);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAverageRating(Long courseId) {
        courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        Double avg = feedbackRepository.getAverageRatingForCourse(courseId);
        Long count = feedbackRepository.countByCourseId(courseId);
        double safeAvg = avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;

        return Map.of(
                "courseId", courseId,
                "averageRating", safeAvg,
                "totalReviews", count != null ? count : 0L
        );
    }
}
