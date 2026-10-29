package com.MOOC.OnlineLearningPlatfrom.Repository;

import com.MOOC.OnlineLearningPlatfrom.Entity.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestRepository extends JpaRepository<Test, Long> {
    List<Test> findByCourse_Id(Long courseId);
    List<Test> findByLecture_Id(Long lectureId);
    List<Test> findByCourse_IdAndIsRemedial(Long courseId, Boolean isRemedial);

    List<Test> findByContent_ContentId(Long contentId);
    Optional<Test> findFirstByContent_ContentId(Long contentId);
    Optional<Test> findFirstByLecture_Id(Long lectureId);
}
