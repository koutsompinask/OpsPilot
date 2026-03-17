package com.opspilot.assistant.service.chunking;

public record TextChunk(
        String text,
        String sectionTitle,
        String chunkType
) {
}
