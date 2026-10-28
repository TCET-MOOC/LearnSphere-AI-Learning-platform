-- Flyway Migration V9: Opt-in YouTube Publish & Channel Connections (W2-13)
-- Rules: A-25, BR-26, NFR-03, spec §6

CREATE TABLE IF NOT EXISTS youtube_channel_connections (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    channel_id VARCHAR(128),
    channel_title VARCHAR(255),
    encrypted_refresh_token TEXT NOT NULL,
    access_token TEXT,
    token_expires_at TIMESTAMP,
    connected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_youtube_channel_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_youtube_channel_user ON youtube_channel_connections(user_id);

CREATE TABLE IF NOT EXISTS media_publish_jobs (
    job_id BIGSERIAL PRIMARY KEY,
    content_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    privacy_status VARCHAR(32) NOT NULL DEFAULT 'unlisted',
    youtube_video_id VARCHAR(128),
    error_message TEXT,
    idempotency_key VARCHAR(128),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_publish_jobs_content FOREIGN KEY (content_id) REFERENCES course_content(content_id) ON DELETE CASCADE,
    CONSTRAINT fk_publish_jobs_teacher FOREIGN KEY (teacher_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_publish_jobs_content ON media_publish_jobs(content_id);
CREATE INDEX IF NOT EXISTS idx_publish_jobs_teacher ON media_publish_jobs(teacher_id);
CREATE INDEX IF NOT EXISTS idx_publish_jobs_status ON media_publish_jobs(status);
