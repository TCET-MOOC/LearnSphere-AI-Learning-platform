-- V12: Add unique constraint on certificates (user_id, course_id, type) for idempotent issuance

DELETE FROM public.certificates c
WHERE c.id IN (
    SELECT id FROM (
        SELECT id,
               ROW_NUMBER() OVER (
                   PARTITION BY user_id, course_id, type
                   ORDER BY issued_at ASC, id ASC
               ) as rn
        FROM public.certificates
        WHERE user_id IS NOT NULL AND course_id IS NOT NULL AND type IS NOT NULL
    ) ranked
    WHERE ranked.rn > 1
);

ALTER TABLE public.certificates
    ADD CONSTRAINT uq_certificates_user_course_type UNIQUE (user_id, course_id, type);

CREATE INDEX IF NOT EXISTS idx_certificates_user_course_type
    ON public.certificates(user_id, course_id, type);
