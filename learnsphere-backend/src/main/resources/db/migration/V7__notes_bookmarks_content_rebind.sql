-- V7: Rebind notes and bookmarks to CourseContent (W2-08, A-15, AC-14)

ALTER TABLE notes
    ADD COLUMN IF NOT EXISTS content_id BIGINT REFERENCES course_content(content_id) ON DELETE CASCADE;

ALTER TABLE bookmarks
    ADD COLUMN IF NOT EXISTS content_id BIGINT REFERENCES course_content(content_id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_notes_content_id ON notes(content_id);
CREATE INDEX IF NOT EXISTS idx_bookmarks_content_id ON bookmarks(content_id);

-- Backfill content_id from legacy lecture_id via course_content.legacy_lecture_id
UPDATE notes n
SET content_id = cc.content_id
FROM course_content cc
WHERE n.lecture_id = cc.legacy_lecture_id AND n.content_id IS NULL;

UPDATE bookmarks b
SET content_id = cc.content_id
FROM course_content cc
WHERE b.lecture_id = cc.legacy_lecture_id AND b.content_id IS NULL;
