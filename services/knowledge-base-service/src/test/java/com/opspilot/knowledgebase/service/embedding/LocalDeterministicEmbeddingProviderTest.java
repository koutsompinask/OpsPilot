package com.opspilot.knowledgebase.service.embedding;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class LocalDeterministicEmbeddingProviderTest {

    @Test
    void shouldReturnStable1536DimensionVectors() {
        LocalDeterministicEmbeddingProvider provider = new LocalDeterministicEmbeddingProvider();

        List<List<Double>> first = provider.embed(List.of("hello world"));
        List<List<Double>> second = provider.embed(List.of("hello world"));

        assertThat(first).hasSize(1);
        assertThat(first.get(0)).hasSize(1536);
        assertThat(first).isEqualTo(second);
    }
}
