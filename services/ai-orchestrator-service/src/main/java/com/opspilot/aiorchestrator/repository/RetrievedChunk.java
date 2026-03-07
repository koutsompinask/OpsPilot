package com.opspilot.aiorchestrator.repository;

import java.util.UUID;

public record RetrievedChunk(
        UUID documentId,
        String documentName,
        int chunkIndex,
        String chunkText,
        double distance
) {
}
