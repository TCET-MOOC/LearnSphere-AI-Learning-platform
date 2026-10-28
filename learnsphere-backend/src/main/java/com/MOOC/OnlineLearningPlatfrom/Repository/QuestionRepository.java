package com.MOOC.OnlineLearningPlatfrom.Repository;

import com.MOOC.OnlineLearningPlatfrom.Entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByTest_TestId(Long testId);
}