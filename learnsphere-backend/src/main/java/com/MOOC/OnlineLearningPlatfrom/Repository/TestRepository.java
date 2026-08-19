package com.MOOC.OnlineLearningPlatfrom.Repository;

import com.MOOC.OnlineLearningPlatfrom.Entity.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestRepository extends JpaRepository<Test, Long> {
    List<Test> findByCourse_Id(Long courseId);
    List<Test> findByLecture_Id(Long lectureId);
    List<Test> findByCourse_IdAndIsRemedial(Long courseId, Boolean isRemedial);
}
