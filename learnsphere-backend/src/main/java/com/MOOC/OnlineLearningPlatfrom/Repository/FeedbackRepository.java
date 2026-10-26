package com.MOOC.OnlineLearningPlatfrom.Repository;

import com.MOOC.OnlineLearningPlatfrom.Entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findByCourse_IdOrderByCreatedAtDesc(Long courseId);
    Optional<Feedback> findByUser_UserIdAndCourse_Id(Long userId, Long courseId);

    @Query("SELECT AVG(f.rating) FROM Feedback f WHERE f.course.id = :courseId")
    Double getAverageRatingForCourse(@Param("courseId") Long courseId);

    @Query("SELECT COUNT(f) FROM Feedback f WHERE f.course.id = :courseId")
    Long countByCourseId(@Param("courseId") Long courseId);
}
