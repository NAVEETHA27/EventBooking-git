-- EventGPT additive schema migration.
-- This script does not alter or drop existing Event Booking System tables.

CREATE TABLE IF NOT EXISTS chat_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NULL,
    user_role VARCHAR(32) NULL,
    title VARCHAR(180) NULL,
    messages_json LONGTEXT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_chat_sessions_user (user_id),
    INDEX idx_chat_sessions_updated (updated_at)
);

CREATE TABLE IF NOT EXISTS ai_vector_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_type VARCHAR(60) NOT NULL,
    source_id VARCHAR(120) NOT NULL,
    chunk_index INT NOT NULL,
    title VARCHAR(220) NOT NULL,
    content LONGTEXT NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    embedding_json LONGTEXT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_ai_vector_chunk (source_type, source_id, chunk_index),
    INDEX idx_ai_vector_source (source_type, source_id),
    INDEX idx_ai_vector_hash (content_hash)
);

ALTER TABLE events
    ADD COLUMN IF NOT EXISTS certificate_template_url VARCHAR(500) NULL,
    ADD COLUMN IF NOT EXISTS certificate_signature_name VARCHAR(160) NULL;
