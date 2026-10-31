-- Migration V22: Expand flagged_content_content_type_check to allow DISCUSSION_THREAD (W5-04, [E-F7, E-F8])

ALTER TABLE public.flagged_content DROP CONSTRAINT IF EXISTS flagged_content_content_type_check;
ALTER TABLE public.flagged_content ADD CONSTRAINT flagged_content_content_type_check CHECK (((content_type)::text = ANY ((ARRAY['DISCUSSION_POST'::character varying, 'MESSAGE'::character varying, 'DISCUSSION_THREAD'::character varying])::text[])));

