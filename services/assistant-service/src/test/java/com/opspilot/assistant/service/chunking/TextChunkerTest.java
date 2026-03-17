package com.opspilot.assistant.service.chunking;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class TextChunkerTest {

    @Test
    void shouldCreateStructuredChunksFromPolicyText() {
        TextChunker chunker = new TextChunker(180, 1);

        List<TextChunk> chunks = chunker.chunk("""
                Front Desk Operations

                Check-in time starts at 15:00 local time.
                Check-out time is 11:00 local time.

                Early check-in policy:
                - Subject to room availability.
                - Before 12:00 may incur a 20 EUR fee.
                """);

        assertThat(chunks).isNotEmpty();
        assertThat(chunks.stream().map(TextChunk::sectionTitle)).contains("Front Desk Operations");
        assertThat(chunks.stream().map(TextChunk::text).toList()).anySatisfy(text -> {
            assertThat(text).contains("Check-in");
            assertThat(text).contains("Check-out");
        });
    }

    @Test
    void shouldReturnEmptyForBlankText() {
        TextChunker chunker = new TextChunker(180, 1);
        assertThat(chunker.chunk("   ")).isEmpty();
    }
}
