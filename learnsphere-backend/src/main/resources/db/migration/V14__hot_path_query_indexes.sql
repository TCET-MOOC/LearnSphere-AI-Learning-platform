-- V14__hot_path_query_indexes.sql
-- W3-13: Hot-path read optimization indexes (NFR-02, NFR-03)
-- Covers user_progress, enrollments, leaderboard, test_attempts, and course_content

-- 1. Enrollments indexes for student course listing and course rosters
CREATE INDEX IF NOT EXISTS idx_enrollments_user_id
    ON enrollments (user_id);

CREATE INDEX IF NOT EXISTS idx_enrollments_course_id
    ON enrollments (course_id);

-- 2. User Progress indexes for user-scoped progress lookups and completion checks
CREATE INDEX IF NOT EXISTS idx_user_progress_user_id
    ON user_progress (user_id);

CREATE INDEX IF NOT EXISTS idx_user_progress_completed
    ON user_progress (user_id, completed_at);

CREATE INDEX IF NOT EXISTS idx_user_progress_lecture_id
    ON user_progress (lecture_id);

-- 3. Leaderboard indexes for ranked course leaderboards and user rank lookups
CREATE INDEX IF NOT EXISTS idx_leaderboard_course_avg_score
    ON leaderboard (course_id, avg_score DESC);

CREATE INDEX IF NOT EXISTS idx_leaderboard_user_id
    ON leaderboard (user_id);

-- 4. Test attempts indexes for user attempt history and test attempt lookups
CREATE INDEX IF NOT EXISTS idx_test_attempts_user_id
    ON test_attempts (user_id);

CREATE INDEX IF NOT EXISTS idx_test_attempts_test_id
    ON test_attempts (test_id);

CREATE INDEX IF NOT EXISTS idx_test_attempts_user_test
    ON test_attempts (user_id, test_id);

-- 5. Course content module and position ordering index (PLAN §8 / §11)
CREATE INDEX IF NOT EXISTS idx_course_content_module_pos
    ON course_content (module_id, position);
