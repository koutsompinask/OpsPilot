package com.opspilot.knowledgebase.service.chunking;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class TextChunkerTest {

    @Test
    void shouldSplitTextWithOverlap() {
        TextChunker chunker = new TextChunker(10, 2);

        List<String> chunks = chunker.chunk("abcdefghijklmnopqrstuvwxyz");

        assertThat(chunks).hasSize(4);
        assertThat(chunks.get(0)).isEqualTo("abcdefghij");
        assertThat(chunks.get(1)).isEqualTo("ijklmnopqr");
        assertThat(chunks.get(2)).isEqualTo("qrstuvwxyz");
    }

    @Test
    void shouldReturnEmptyForBlankText() {
        TextChunker chunker = new TextChunker(10, 2);
        assertThat(chunker.chunk("   ")).isEmpty();
    }
}
