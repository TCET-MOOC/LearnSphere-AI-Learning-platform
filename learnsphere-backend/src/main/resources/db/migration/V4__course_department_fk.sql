-- V4__course_department_fk.sql
-- W2-01 / M-01: Add courses.department_id FK, backfill from courses.department, add index.

-- 1. Add department_id column
ALTER TABLE public.courses ADD COLUMN department_id bigint;

-- 2. Backfill missing departments into public.departments if any distinct department name in courses doesn't exist
INSERT INTO public.departments (name, created_at)
SELECT DISTINCT TRIM(c.department), NOW()
FROM public.courses c
WHERE c.department IS NOT NULL
  AND TRIM(c.department) <> ''
  AND NOT EXISTS (
      SELECT 1 FROM public.departments d
      WHERE LOWER(TRIM(d.name)) = LOWER(TRIM(c.department))
  );

-- 3. Backfill courses.department_id from matching departments.name
UPDATE public.courses c
SET department_id = d.department_id
FROM public.departments d
WHERE LOWER(TRIM(c.department)) = LOWER(TRIM(d.name))
  AND c.department_id IS NULL;

-- 4. If any course still has NULL department_id, create or assign default 'General' department
DO $$
DECLARE
    default_dept_id bigint;
BEGIN
    IF EXISTS (SELECT 1 FROM public.courses WHERE department_id IS NULL) THEN
        SELECT department_id INTO default_dept_id FROM public.departments WHERE LOWER(name) = 'general' LIMIT 1;
        IF default_dept_id IS NULL THEN
            INSERT INTO public.departments (name, created_at) VALUES ('General', NOW()) RETURNING department_id INTO default_dept_id;
        END IF;
        UPDATE public.courses SET department_id = default_dept_id WHERE department_id IS NULL;
    END IF;
END $$;

-- 5. Add foreign key constraint and index
ALTER TABLE public.courses
    ADD CONSTRAINT fk_courses_department
    FOREIGN KEY (department_id)
    REFERENCES public.departments (department_id)
    ON DELETE RESTRICT;

CREATE INDEX idx_courses_department_id ON public.courses (department_id);
