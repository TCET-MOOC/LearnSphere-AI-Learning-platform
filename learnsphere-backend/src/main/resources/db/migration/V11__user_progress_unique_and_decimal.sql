-- W3-03: Unique (user_id, content_id) on user_progress; progress_percent NUMERIC(5,2); clamp 0-100 (AC-11, AC-12, A-08, A-10)

-- 1. Backfill content_id from course_content if any legacy row has content_id NULL but lecture_id NOT NULL
UPDATE public.user_progress up
SET content_id = cc.content_id
FROM public.course_content cc
WHERE up.content_id IS NULL
  AND up.lecture_id IS NOT NULL
  AND up.lecture_id = cc.legacy_lecture_id;

-- 2. Deduplicate existing rows for (user_id, content_id) keeping latest progress
DELETE FROM public.user_progress up1
WHERE up1.content_id IS NOT NULL
  AND EXISTS (
    SELECT 1 FROM public.user_progress up2
    WHERE up2.user_id = up1.user_id
      AND up2.content_id = up1.content_id
      AND (
        up2.progress_percent > up1.progress_percent
        OR (up2.progress_percent = up1.progress_percent AND up2.progress_id > up1.progress_id)
      )
  );

-- 3. Clamp existing values
UPDATE public.user_progress
SET progress_percent = 100
WHERE progress_percent > 100;

UPDATE public.user_progress
SET progress_percent = 0
WHERE progress_percent < 0;

-- 4. Alter progress_percent to NUMERIC(5,2)
ALTER TABLE public.user_progress
ALTER COLUMN progress_percent TYPE NUMERIC(5,2) USING progress_percent::NUMERIC(5,2);

-- 5. Add unique constraint on (user_id, content_id)
ALTER TABLE ONLY public.user_progress
ADD CONSTRAINT uq_user_progress_user_content UNIQUE (user_id, content_id);

-- 6. Add index on (user_id, content_id)
CREATE INDEX IF NOT EXISTS idx_user_progress_user_content ON public.user_progress (user_id, content_id);
