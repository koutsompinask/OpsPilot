package com.opspilot.assistant.domain.entity;

/**
 * Lifecycle status of a {@link Document}.
 *
 * <ul>
 *   <li>{@code PROCESSING} — the file has been accepted but ingestion (chunking + embedding) is still running</li>
 *   <li>{@code READY} — ingestion completed successfully; the document's chunks are indexed and searchable</li>
 *   <li>{@code FAILED} — ingestion encountered an unrecoverable error; the document is not searchable</li>
 * </ul>
 */
public enum DocumentStatus {
    PROCESSING,
    READY,
    FAILED
}
