ALTER TABLE assistant.document_chunks
    ADD COLUMN IF NOT EXISTS section_title VARCHAR(255) NOT NULL DEFAULT 'General',
    ADD COLUMN IF NOT EXISTS chunk_type VARCHAR(32) NOT NULL DEFAULT 'paragraph';

CREATE INDEX IF NOT EXISTS idx_document_chunks_lexical_search
    ON assistant.document_chunks
    USING GIN (
        to_tsvector(
            'english',
            coalesce(section_title, '') || ' ' || coalesce(chunk_text, '')
        )
    );
