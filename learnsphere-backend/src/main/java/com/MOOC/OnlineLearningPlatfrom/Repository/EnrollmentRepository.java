package com.MOOC.OnlineLearningPlatfrom.Repository;

import com.MOOC.OnlineLearningPlatfrom.Entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByUserId(Long userId);
    Optional<Enrollment> findByUserIdAndCourse_Id(Long userId, Long courseId);
    boolean existsByUserIdAndCourse_Id(Long userId, Long courseId);
    Optional<Enrollment> findByUserIdAndCourse_IdAndRole(Long userId, Long courseId, String role);
    boolean existsByUserIdAndCourse_IdAndRole(Long userId, Long courseId, String role);
    List<Enrollment> findByCourse_Id(Long courseId);
    long countByCourse_Id(Long courseId);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.course.price IS NOT NULL AND e.course.price > 0")
    long countPaidEnrollments();
}
