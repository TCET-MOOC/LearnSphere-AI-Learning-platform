-- Migration for W4-08 / A-10: Numeric types for assessments
-- Questions marks, StudentAnswer marks_awarded, and TestAttempt score -> NUMERIC(8,2)

ALTER TABLE public.questions
    ALTER COLUMN marks TYPE NUMERIC(8,2) USING marks::NUMERIC(8,2);

ALTER TABLE public.student_answers
    ALTER COLUMN marks_awarded TYPE NUMERIC(8,2) USING marks_awarded::NUMERIC(8,2);

ALTER TABLE public.test_attempts
    ALTER COLUMN score TYPE NUMERIC(8,2) USING score::NUMERIC(8,2);

