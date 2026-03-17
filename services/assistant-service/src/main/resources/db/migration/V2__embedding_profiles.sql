ALTER TABLE assistant.documents
    ADD COLUMN IF NOT EXISTS embedding_profile VARCHAR(255) NOT NULL DEFAULT 'legacy-local-1536';

ALTER TABLE assistant.document_chunks
    ALTER COLUMN embedding TYPE vector USING embedding::vector;
