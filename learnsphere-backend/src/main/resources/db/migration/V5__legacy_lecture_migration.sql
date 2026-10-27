-- V5__legacy_lecture_migration.sql
-- W2-05 / M-02: Lecture -> CourseContent 1:1 migration; copy lectures -> course_content; backfill user_progress; keep lectures read-only (A-05, [B-F1]).

-- 1. Add legacy_lecture_id column to course_content
ALTER TABLE public.course_content ADD COLUMN legacy_lecture_id bigint;

-- 2. Add unique partial index for legacy_lecture_id
CREATE UNIQUE INDEX uq_course_content_legacy_lecture
    ON public.course_content (legacy_lecture_id)
    WHERE legacy_lecture_id IS NOT NULL;

-- 3. Ensure every course with lectures has at least one module
INSERT INTO public.module (course_id, title, position, created_at)
SELECT DISTINCT l.course_id, 'Lectures', 1, NOW()
FROM public.lectures l
WHERE l.course_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM public.module m WHERE m.course_id = l.course_id
  );

-- 4. Copy existing lectures into course_content (1:1 canonical migration)
INSERT INTO public.course_content (module_id, content_type, title, file_path, duration_seconds, position, uploaded_at, legacy_lecture_id)
SELECT 
    (SELECT m.module_id FROM public.module m WHERE m.course_id = l.course_id ORDER BY m.position ASC LIMIT 1) AS module_id,
    'VIDEO' AS content_type,
    COALESCE(l.title, 'Lecture ' || COALESCE(l.number, 1)),
    l.video_url,
    COALESCE(l.duration, 0) AS duration_seconds,
    COALESCE(l.number, 1) AS position,
    NOW() AS uploaded_at,
    l.id AS legacy_lecture_id
FROM public.lectures l
WHERE l.course_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM public.course_content cc WHERE cc.legacy_lecture_id = l.id
  );

-- 5. Backfill user_progress content_id from migrated course_content
UPDATE public.user_progress up
SET content_id = cc.content_id
FROM public.course_content cc
WHERE up.content_id IS NULL
  AND up.lecture_id IS NOT NULL
  AND up.lecture_id = cc.legacy_lecture_id;
