-- V8: Canonical media source schema (W2-12, A-25, spec §6)

ALTER TABLE course_content ADD COLUMN IF NOT EXISTS source_type VARCHAR(32);
ALTER TABLE course_content ADD COLUMN IF NOT EXISTS provider_video_id VARCHAR(128);
ALTER TABLE course_content ADD COLUMN IF NOT EXISTS storage_key VARCHAR(128);
ALTER TABLE course_content ADD COLUMN IF NOT EXISTS external_url TEXT;
ALTER TABLE course_content ADD COLUMN IF NOT EXISTS media_status VARCHAR(32) DEFAULT 'READY';
ALTER TABLE course_content ADD COLUMN IF NOT EXISTS mime_type VARCHAR(128);
ALTER TABLE course_content ADD COLUMN IF NOT EXISTS size_bytes BIGINT;

CREATE INDEX IF NOT EXISTS idx_course_content_module_position ON course_content(module_id, position);
CREATE INDEX IF NOT EXISTS idx_course_content_storage_key ON course_content(storage_key);

-- Backfill existing rows where content_type = 'VIDEO'
-- If file_path contains youtube.com or youtu.be, extract video id or populate source_type = 'YOUTUBE'
UPDATE course_content
SET source_type = 'YOUTUBE',
    provider_video_id = substring(file_path from '(?:v=|/embed/|youtu\.be/|/v/|/e/|watch\?v=)([a-zA-Z0-9_-]{11})'),
    media_status = 'READY'
WHERE content_type = 'VIDEO' AND (file_path LIKE '%youtube.com%' OR file_path LIKE '%youtu.be%');

-- If file_path points to /api/files/{key}
UPDATE course_content
SET source_type = 'UPLOAD',
    storage_key = substring(file_path from '/api/files/([a-zA-Z0-9_-]+)'),
    media_status = 'READY'
WHERE content_type = 'VIDEO' AND source_type IS NULL AND file_path LIKE '%/api/files/%';

-- If other http/https
UPDATE course_content
SET source_type = 'EXTERNAL',
    external_url = file_path,
    media_status = 'READY'
WHERE content_type = 'VIDEO' AND source_type IS NULL AND (file_path LIKE 'http://%' OR file_path LIKE 'https://%');

-- Any remaining non-empty file_path
UPDATE course_content
SET source_type = 'EXTERNAL',
    external_url = file_path,
    media_status = 'READY'
WHERE content_type = 'VIDEO' AND source_type IS NULL AND file_path IS NOT NULL AND file_path <> '';
