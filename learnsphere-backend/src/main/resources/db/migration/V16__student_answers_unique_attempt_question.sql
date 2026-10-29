-- W4-04: Enforce unique (attempt_id, question_id) on student_answers with upsert semantics

-- Deduplicate any existing duplicate student_answers per (attempt_id, question_id), keeping the latest
DELETE FROM public.student_answers
WHERE student_answer_id NOT IN (
    SELECT MAX(student_answer_id)
    FROM public.student_answers
    GROUP BY attempt_id, question_id
);

-- Add unique constraint on (attempt_id, question_id)
ALTER TABLE public.student_answers
    ADD CONSTRAINT uq_student_answers_attempt_question UNIQUE (attempt_id, question_id);

-- Create index for performance
CREATE INDEX IF NOT EXISTS idx_student_answers_attempt_question
    ON public.student_answers (attempt_id, question_id);

