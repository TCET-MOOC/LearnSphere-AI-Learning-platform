package com.MOOC.OnlineLearningPlatfrom.Repository;

import com.MOOC.OnlineLearningPlatfrom.Entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByUserId(Long userId);
    Optional<Enrollment> findByUserIdAndCourse_Id(Long userId, Long courseId);
    boolean existsByUserIdAndCourse_Id(Long userId, Long courseId);
    List<Enrollment> findByCourse_Id(Long courseId);
    long countByCourse_Id(Long courseId);
}
