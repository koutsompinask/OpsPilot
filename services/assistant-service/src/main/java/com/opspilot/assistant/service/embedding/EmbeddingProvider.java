package com.opspilot.assistant.service.embedding;

import java.util.List;

/**
 * Contract for all embedding back-ends supported by the assistant-service.
 *
 * <p>Implementations are selected at startup via the {@code assistant.embedding.provider}
 * property and wrapped by {@link EmbeddingService}, which delegates all calls to the
 * active provider. Each implementation must also expose an {@link EmbeddingProfile} so
 * that dimension-mismatch issues can be detected at startup and stored alongside
 * document chunks for future re-indexing decisions.</p>
 */
public interface EmbeddingProvider {

    /**
     * Returns the profile that describes this provider's model identity and output dimensions.
     *
     * @return an {@link EmbeddingProfile} record for the active model
     */
    EmbeddingProfile profile();

    /**
     * Converts a batch of text inputs into dense embedding vectors.
     *
     * @param inputs the texts to embed; must be non-null and non-empty
     * @return a list of vectors, one per input, each of length {@link EmbeddingProfile#dimensions()}
     */
    List<List<Double>> embed(List<String> inputs);
}
