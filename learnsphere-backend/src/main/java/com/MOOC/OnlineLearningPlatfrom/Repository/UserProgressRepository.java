package com.MOOC.OnlineLearningPlatfrom.Repository;

import com.MOOC.OnlineLearningPlatfrom.Entity.UserProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProgressRepository extends JpaRepository<UserProgress, Long> {
    Optional<UserProgress> findByUserIdAndLecture_Id(Long userId, Long lectureId);
    Optional<UserProgress> findByUserIdAndContent_ContentId(Long userId, Long contentId);
    List<UserProgress> findByUserId(Long userId);
    List<UserProgress> findByUserIdAndLecture_Course_Id(Long userId, Long courseId);

    @Query("SELECT up FROM UserProgress up " +
           "LEFT JOIN up.content c " +
           "LEFT JOIN c.module m " +
           "LEFT JOIN m.course mc " +
           "LEFT JOIN up.lecture l " +
           "LEFT JOIN l.course lc " +
           "WHERE up.userId = :userId AND (mc.id = :courseId OR lc.id = :courseId)")
    List<UserProgress> findByUserIdAndCourseId(@Param("userId") Long userId, @Param("courseId") Long courseId);

    @Query("SELECT up FROM UserProgress up " +
           "LEFT JOIN up.content c " +
           "LEFT JOIN c.module m " +
           "LEFT JOIN m.course mc " +
           "LEFT JOIN up.lecture l " +
           "LEFT JOIN l.course lc " +
           "WHERE (mc.id = :courseId OR lc.id = :courseId)")
    List<UserProgress> findByCourseId(@Param("courseId") Long courseId);
}
