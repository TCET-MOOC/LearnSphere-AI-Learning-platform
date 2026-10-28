-- V10__unique_enrollment.sql
-- W3-01: Unique constraint on (user_id, course_id, role)
-- Deduplicate any existing duplicate rows keeping the one with the smallest enrollment_id

DELETE FROM enrollments e1
WHERE e1.enrollment_id > (
    SELECT MIN(e2.enrollment_id)
    FROM enrollments e2
    WHERE e2.user_id = e1.user_id
      AND e2.course_id = e1.course_id
      AND COALESCE(e2.role, 'STUDENT') = COALESCE(e1.role, 'STUDENT')
);

UPDATE enrollments
SET role = 'STUDENT'
WHERE role IS NULL OR TRIM(role) = '';

ALTER TABLE enrollments
    ALTER COLUMN role SET DEFAULT 'STUDENT',
    ALTER COLUMN role SET NOT NULL;

ALTER TABLE enrollments
    ADD CONSTRAINT uq_enrollments_user_course_role UNIQUE (user_id, course_id, role);

CREATE INDEX IF NOT EXISTS idx_enrollments_user_course_role
    ON enrollments (user_id, course_id, role);
