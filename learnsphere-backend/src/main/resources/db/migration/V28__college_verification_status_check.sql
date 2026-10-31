-- Migration V28: Allow REJECTED verification status for colleges (W6-10)
ALTER TABLE public.colleges DROP CONSTRAINT IF EXISTS colleges_verification_status_check;
ALTER TABLE public.colleges ADD CONSTRAINT colleges_verification_status_check
    CHECK (((verification_status)::text = ANY ((ARRAY['VERIFIED'::character varying, 'PENDING'::character varying, 'REJECTED'::character varying])::text[])));

