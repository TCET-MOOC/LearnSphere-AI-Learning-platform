-- Migration M-11: test_attempts lifecycle timestamps and status constraint, tests.created_at
-- References: AC-18, [D-F5, D-F11], LMS Entity Reference Section 6

-- 1. Add started_at and completed_at to test_attempts
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'test_attempts' AND column_name = 'started_at'
    ) THEN
        ALTER TABLE public.test_attempts ADD COLUMN started_at TIMESTAMP WITHOUT TIME ZONE;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'test_attempts' AND column_name = 'completed_at'
    ) THEN
        ALTER TABLE public.test_attempts ADD COLUMN completed_at TIMESTAMP WITHOUT TIME ZONE;
    END IF;
END $$;

-- 2. Backfill started_at from attempted_at
UPDATE public.test_attempts
SET started_at = attempted_at
WHERE started_at IS NULL AND attempted_at IS NOT NULL;

UPDATE public.test_attempts
SET started_at = CURRENT_TIMESTAMP
WHERE started_at IS NULL;

-- Backfill completed_at for already completed attempts
UPDATE public.test_attempts
SET completed_at = attempted_at
WHERE completed_at IS NULL AND UPPER(status) = 'COMPLETED';

-- 3. Constrain status to ('IN_PROGRESS', 'COMPLETED', 'ABANDONED')
UPDATE public.test_attempts
SET status = 'COMPLETED'
WHERE status IS NULL OR status NOT IN ('IN_PROGRESS', 'COMPLETED', 'ABANDONED');

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_test_attempts_status'
    ) THEN
        ALTER TABLE public.test_attempts
        ADD CONSTRAINT chk_test_attempts_status
        CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'ABANDONED'));
    END IF;
END $$;

-- 4. Index on (status, started_at) for efficient background timeout worker scanning
CREATE INDEX IF NOT EXISTS idx_test_attempts_status_started
    ON public.test_attempts (status, started_at);

-- 5. Add created_at to tests table
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'tests' AND column_name = 'created_at'
    ) THEN
        ALTER TABLE public.tests ADD COLUMN created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP;
    END IF;
END $$;

UPDATE public.tests
SET created_at = CURRENT_TIMESTAMP
WHERE created_at IS NULL;

