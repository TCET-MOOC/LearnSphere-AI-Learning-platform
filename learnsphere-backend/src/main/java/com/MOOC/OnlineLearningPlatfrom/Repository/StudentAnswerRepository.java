package com.MOOC.OnlineLearningPlatfrom.Repository;

import com.MOOC.OnlineLearningPlatfrom.Entity.StudentAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentAnswerRepository extends JpaRepository<StudentAnswer, Long> {
    List<StudentAnswer> findByAttempt_AttemptId(Long attemptId);

    /**
     * TEXT answers that still need a manual grade (W7-16): the attempt is finished
     * (COMPLETED or ABANDONED) and no marks are stored yet. Oldest submission first.
     */
    @org.springframework.data.jpa.repository.Query("""
            select a from StudentAnswer a
            join fetch a.attempt t
            join fetch a.question q
            join fetch t.test tst
            where upper(q.questionType) = 'TEXT'
              and a.marksAwarded is null
              and upper(t.status) in ('COMPLETED', 'ABANDONED')
              and tst.testId in :testIds
            order by t.completedAt asc, a.studentAnswerId asc
            """)
    List<StudentAnswer> findUngradedTextAnswers(@org.springframework.data.repository.query.Param("testIds") java.util.Collection<Long> testIds);
    Optional<StudentAnswer> findByAttempt_AttemptIdAndQuestion_QuestionId(Long attemptId, Long questionId);
}