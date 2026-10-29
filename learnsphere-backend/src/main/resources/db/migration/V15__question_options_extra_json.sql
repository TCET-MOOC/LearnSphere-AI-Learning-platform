-- V15__question_options_extra_json.sql
-- W4-03: Migrate question_options to questions.extra as JSON array; drop question_options table (A-12, [D-F8])

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'question_options') THEN
        -- 1. Migrate options into questions.extra if extra is null or empty
        WITH aggregated_options AS (
            SELECT
                question_id,
                json_agg(option_text)::text AS options_json
            FROM public.question_options
            GROUP BY question_id
        )
        UPDATE public.questions q
        SET extra = ao.options_json
        FROM aggregated_options ao
        WHERE q.question_id = ao.question_id
          AND (q.extra IS NULL OR trim(q.extra) = '' OR trim(q.extra) = '[]');

        -- 2. Drop the legacy question_options collection table
        DROP TABLE public.question_options CASCADE;
    END IF;
END $$;
