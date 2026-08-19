package com.MOOC.OnlineLearningPlatfrom.Repository;

import com.MOOC.OnlineLearningPlatfrom.Entity.UserProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProgressRepository extends JpaRepository<UserProgress, Long> {
    Optional<UserProgress> findByUserIdAndLecture_Id(Long userId, Long lectureId);
    List<UserProgress> findByUserId(Long userId);
    List<UserProgress> findByUserIdAndLecture_Course_Id(Long userId, Long courseId);
}
