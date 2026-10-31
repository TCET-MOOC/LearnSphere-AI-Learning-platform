-- Migration V23: Add course scoping and expiry to announcements (W5-05, A-17, [E-F13])

ALTER TABLE public.announcements ADD COLUMN IF NOT EXISTS course_id bigint;
ALTER TABLE public.announcements ADD COLUMN IF NOT EXISTS expires_at timestamp(6) without time zone;

ALTER TABLE public.announcements DROP CONSTRAINT IF EXISTS fk_announcements_course;
ALTER TABLE public.announcements ADD CONSTRAINT fk_announcements_course FOREIGN KEY (course_id) REFERENCES public.courses(id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_announcements_course_id ON public.announcements(course_id);
CREATE INDEX IF NOT EXISTS idx_announcements_expires_at ON public.announcements(expires_at);
CREATE INDEX IF NOT EXISTS idx_announcements_pinned_created ON public.announcements(pinned DESC, created_at DESC);

