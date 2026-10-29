-- Migration for W4-11: Player-attached quiz end-to-end and content binding
-- Add content_id, placement, required_for_completion, pass_threshold to tests table.
-- Default pass_threshold to 60.00, default placement to 'POST_VIDEO', default max_attempts to 3.

ALTER TABLE public.tests
    ADD COLUMN IF NOT EXISTS content_id BIGINT REFERENCES public.course_content(content_id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS placement VARCHAR(32) DEFAULT 'POST_VIDEO',
    ADD COLUMN IF NOT EXISTS required_for_completion BOOLEAN DEFAULT false,
    ADD COLUMN IF NOT EXISTS pass_threshold NUMERIC(5,2) DEFAULT 60.00;

-- Backfill content_id from lecture_id where course_content.legacy_lecture_id matches
UPDATE public.tests t
SET content_id = c.content_id
FROM public.course_content c
WHERE c.legacy_lecture_id = t.lecture_id AND t.content_id IS NULL;

-- Backfill default max_attempts to 3 where null
UPDATE public.tests
SET max_attempts = 3
WHERE max_attempts IS NULL;

-- Index for fast lookup by content_id
CREATE INDEX IF NOT EXISTS idx_tests_content_id ON public.tests(content_id);

