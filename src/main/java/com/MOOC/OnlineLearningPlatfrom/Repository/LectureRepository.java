package com.MOOC.OnlineLearningPlatfrom.Repository;

import com.MOOC.OnlineLearningPlatfrom.Entity.Lecture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LectureRepository extends JpaRepository<Lecture, Long> {
    List<Lecture> findByCourse_IdOrderByNumberAsc(Long courseId);
    long countByCourse_Id(Long courseId);
}
