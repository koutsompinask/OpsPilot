package com.opspilot.knowledgebase.service.chunking;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TextChunker {

    private final int chunkSize;
    private final int chunkOverlap;

    public TextChunker(
            @Value("${knowledge.chunking.chunk-size:1000}") int chunkSize,
            @Value("${knowledge.chunking.chunk-overlap:200}") int chunkOverlap
    ) {
        if (chunkOverlap < 0 || chunkOverlap >= chunkSize) {
            throw new IllegalArgumentException("Invalid chunking configuration");
        }
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    public List<String> chunk(String text) {
        String normalized = text == null ? "" : text.trim();
        if (normalized.isEmpty()) {
            return List.of();
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        int step = chunkSize - chunkOverlap;
        while (start < normalized.length()) {
            int end = Math.min(normalized.length(), start + chunkSize);
            chunks.add(normalized.substring(start, end));
            start += step;
        }
        return chunks;
    }
}
