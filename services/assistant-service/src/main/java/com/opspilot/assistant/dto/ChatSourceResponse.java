package com.opspilot.assistant.dto;

/**
 * A deduplicated source reference included in the {@link ChatAskResponse}.
 *
 * Unlike {@link ChatEvidenceResponse}, this record omits scores and snippets and is intended
 * for display in the UI as a "Sources" list.
 *
 * @param document the source document filename
 * @param chunkId  a string identifier for the specific chunk ({@code documentId:chunkIndex})
 */
public record ChatSourceResponse(
        String document,
        String chunkId
) {
}
