-- V6: Stored files table for opaque storage keys and safe downloads (W2-07, BR-26)

CREATE TABLE IF NOT EXISTS stored_files (
    id BIGSERIAL PRIMARY KEY,
    file_key VARCHAR(128) NOT NULL UNIQUE,
    owner_id BIGINT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(128) NOT NULL,
    size_bytes BIGINT NOT NULL,
    storage_path VARCHAR(512) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_stored_files_file_key ON stored_files(file_key);
CREATE INDEX IF NOT EXISTS idx_stored_files_owner_id ON stored_files(owner_id);
