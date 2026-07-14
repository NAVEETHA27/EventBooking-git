-- EventGPT PGVector schema.
-- Optional vector backend. The main application remains MySQL-compatible by default.

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS eventgpt_vector_documents (
    id BIGSERIAL PRIMARY KEY,
    source_type VARCHAR(60) NOT NULL,
    source_id VARCHAR(120) NOT NULL,
    chunk_index INTEGER NOT NULL,
    title VARCHAR(220) NOT NULL,
    content TEXT NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    embedding VECTOR(96) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_eventgpt_vector_chunk UNIQUE (source_type, source_id, chunk_index)
);

CREATE INDEX IF NOT EXISTS idx_eventgpt_vector_source
    ON eventgpt_vector_documents (source_type, source_id);

CREATE INDEX IF NOT EXISTS idx_eventgpt_vector_hash
    ON eventgpt_vector_documents (content_hash);

CREATE INDEX IF NOT EXISTS idx_eventgpt_vector_embedding
    ON eventgpt_vector_documents
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

