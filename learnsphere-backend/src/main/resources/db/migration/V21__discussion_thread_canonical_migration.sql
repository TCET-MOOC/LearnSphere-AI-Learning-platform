-- Migration V21: Migrate discussion_posts to discussion_thread and post; verify FKs and indexes; drop discussion_posts (A-13, M-10)

-- 1. Ensure columns on discussion_thread and post
ALTER TABLE public.discussion_thread ADD COLUMN IF NOT EXISTS flagged boolean DEFAULT false;
ALTER TABLE public.post ADD COLUMN IF NOT EXISTS flagged boolean DEFAULT false;

-- 2. Migrate existing root posts from discussion_posts into discussion_thread (if table exists)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'discussion_posts') THEN
        INSERT INTO public.discussion_thread (course_id, user_id, title, content, created_at, updated_at, flagged)
        SELECT
            COALESCE(dp.course_id, l.course_id) AS course_id,
            dp.author_id AS user_id,
            CASE
                WHEN length(COALESCE(dp.body, 'Discussion')) <= 60 THEN COALESCE(dp.body, 'Discussion')
                ELSE substring(dp.body from 1 for 57) || '...'
            END AS title,
            COALESCE(dp.body, '') AS content,
            COALESCE(dp.created_at, NOW()) AS created_at,
            COALESCE(dp.created_at, NOW()) AS updated_at,
            COALESCE(dp.flagged, false) AS flagged
        FROM public.discussion_posts dp
        LEFT JOIN public.lectures l ON dp.lecture_id = l.id
        WHERE dp.parent_post_id IS NULL
          AND COALESCE(dp.course_id, l.course_id) IS NOT NULL
          AND dp.author_id IS NOT NULL;

        -- Drop legacy discussion_posts table and constraints
        DROP TABLE public.discussion_posts CASCADE;
    END IF;
END $$;

-- 3. Ensure foreign key constraints with ON DELETE CASCADE
ALTER TABLE public.post DROP CONSTRAINT IF EXISTS fkd4r03wi0mua21qcy0fujfwtpt;
ALTER TABLE public.post DROP CONSTRAINT IF EXISTS fk_post_thread;
ALTER TABLE public.post ADD CONSTRAINT fk_post_thread FOREIGN KEY (thread_id) REFERENCES public.discussion_thread(thread_id) ON DELETE CASCADE;

ALTER TABLE public.post DROP CONSTRAINT IF EXISTS fk7u19g6f38sqcrn2o2ot9l3c4s;
ALTER TABLE public.post DROP CONSTRAINT IF EXISTS fk_post_parent;
ALTER TABLE public.post ADD CONSTRAINT fk_post_parent FOREIGN KEY (parent_post_id) REFERENCES public.post(post_id) ON DELETE CASCADE;

ALTER TABLE public.discussion_thread DROP CONSTRAINT IF EXISTS fkfjd35rl54drl5jjde7ka0b5pd;
ALTER TABLE public.discussion_thread DROP CONSTRAINT IF EXISTS fk_thread_course;
ALTER TABLE public.discussion_thread ADD CONSTRAINT fk_thread_course FOREIGN KEY (course_id) REFERENCES public.courses(id) ON DELETE CASCADE;

-- 4. Create performance indexes
CREATE INDEX IF NOT EXISTS idx_discussion_thread_course_id ON public.discussion_thread(course_id);
CREATE INDEX IF NOT EXISTS idx_discussion_thread_user_id ON public.discussion_thread(user_id);
CREATE INDEX IF NOT EXISTS idx_discussion_thread_created_at ON public.discussion_thread(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_post_thread_id ON public.post(thread_id);
CREATE INDEX IF NOT EXISTS idx_post_user_id ON public.post(user_id);
CREATE INDEX IF NOT EXISTS idx_post_parent_post_id ON public.post(parent_post_id);
CREATE INDEX IF NOT EXISTS idx_post_created_at ON public.post(created_at ASC);

