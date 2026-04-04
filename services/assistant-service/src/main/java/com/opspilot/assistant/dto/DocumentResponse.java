package com.opspilot.assistant.dto;

import com.opspilot.assistant.domain.entity.Document;
import com.opspilot.assistant.domain.entity.DocumentStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * API response representing a tenant's document and its current ingestion state.
 *
 * @param id           the document's unique identifier
 * @param filename     the original uploaded filename
 * @param contentType  the MIME type of the uploaded file
 * @param status       the current ingestion status ({@code PROCESSING}, {@code READY}, or {@code FAILED})
 * @param chunkCount   the number of indexed text chunks; 0 while processing
 * @param errorMessage populated only when {@code status} is {@code FAILED}
 * @param createdAt    when the document was first uploaded
 * @param updatedAt    when the document record was last modified
 */
public record DocumentResponse(
        UUID id,
        String filename,
        String contentType,
        DocumentStatus status,
        Integer chunkCount,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt
) {

    public static DocumentResponse fromEntity(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getOriginalFilename(),
                document.getContentType(),
                document.getStatus(),
                document.getChunkCount(),
                document.getErrorMessage(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }
}
