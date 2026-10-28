package com.MOOC.OnlineLearningPlatfrom.Repository;

import com.MOOC.OnlineLearningPlatfrom.Entity.CourseContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseContentRepository extends JpaRepository<CourseContent, Long> {
    List<CourseContent> findByModule_ModuleIdOrderByPositionAsc(Long moduleId);
    List<CourseContent> findByModule_Course_IdOrderByModule_PositionAscPositionAsc(Long courseId);
    List<CourseContent> findByModule_Course_IdInOrderByModule_PositionAscPositionAsc(java.util.Collection<Long> courseIds);
    java.util.Optional<CourseContent> findByLegacyLectureId(Long legacyLectureId);
}
