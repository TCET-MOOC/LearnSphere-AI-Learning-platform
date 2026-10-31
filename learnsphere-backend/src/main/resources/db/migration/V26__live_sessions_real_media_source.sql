ALTER TABLE public.live_sessions
    ADD COLUMN IF NOT EXISTS source_type VARCHAR(32),
    ADD COLUMN IF NOT EXISTS provider_video_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS source_url TEXT,
    ADD COLUMN IF NOT EXISTS playback_mode VARCHAR(32),
    ADD COLUMN IF NOT EXISTS started_at TIMESTAMP(6),
    ADD COLUMN IF NOT EXISTS ended_at TIMESTAMP(6);

CREATE INDEX IF NOT EXISTS idx_live_sessions_status ON public.live_sessions(status);
