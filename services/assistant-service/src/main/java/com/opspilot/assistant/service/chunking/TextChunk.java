package com.opspilot.assistant.service.chunking;

/**
 * An in-memory representation of a single text chunk produced by {@link TextChunker}.
 *
 * Instances are transient — they exist only during the ingestion pipeline before being
 * persisted as {@code document_chunks} rows with their corresponding vector embeddings.
 *
 * @param text         the raw chunk text that will be embedded and indexed
 * @param sectionTitle the heading or section context this chunk belongs to, may be null
 * @param chunkType    the structural type of the chunk (e.g. {@code "body"}, {@code "heading"})
 */
public record TextChunk(
        String text,
        String sectionTitle,
        String chunkType
) {
}
