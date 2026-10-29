-- Migration M-14 (partial for W4-07): tests.max_attempts
-- References: AC-18, [D-F6, D-F7, D-F10], LMS Entity Reference Section 6

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'tests' AND column_name = 'max_attempts'
    ) THEN
        ALTER TABLE public.tests ADD COLUMN max_attempts INTEGER;
    END IF;
END $$;

