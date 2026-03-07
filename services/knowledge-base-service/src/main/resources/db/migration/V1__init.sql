CREATE EXTENSION IF NOT EXISTS vector;
CREATE SCHEMA IF NOT EXISTS knowledge;

CREATE TABLE IF NOT EXISTS knowledge.documents (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    uploaded_by UUID NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    storage_key VARCHAR(512) NOT NULL,
    status VARCHAR(32) NOT NULL,
    error_message VARCHAR(512),
    chunk_count INT NOT NULL DEFAULT 0,
    created_request_id VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_documents_tenant_created_at ON knowledge.documents (tenant_id, created_at DESC);

CREATE TABLE IF NOT EXISTS knowledge.document_chunks (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES knowledge.documents(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL,
    chunk_index INT NOT NULL,
    chunk_text TEXT NOT NULL,
    embedding VECTOR(1536) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_document_chunks_document_id ON knowledge.document_chunks (document_id);
CREATE INDEX IF NOT EXISTS idx_document_chunks_tenant_id ON knowledge.document_chunks (tenant_id);
