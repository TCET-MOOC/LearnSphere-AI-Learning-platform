-- Migration V25: Add block support to conversations table (W5-08)
ALTER TABLE public.conversations
    ADD COLUMN IF NOT EXISTS is_blocked BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS blocked_by_user_id BIGINT REFERENCES public.users(user_id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_conversations_blocked ON public.conversations(is_blocked);
