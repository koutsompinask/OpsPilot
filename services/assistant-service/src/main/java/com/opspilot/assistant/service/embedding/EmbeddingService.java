package com.opspilot.assistant.service.embedding;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Facade that selects and delegates to the configured {@link EmbeddingProvider} at startup.
 *
 * <p>All four provider beans are always constructed by Spring; the active one is chosen via
 * a switch on {@code assistant.embedding.provider}. This eager-wiring approach avoids
 * conditional bean registration complexity and lets the startup validator probe the selected
 * provider immediately after context refresh.</p>
 */
@Service
public class EmbeddingService {

    // The single active provider, chosen once at construction time
    private final EmbeddingProvider provider;

    public EmbeddingService(
            LocalDeterministicEmbeddingProvider localProvider,
            TeiEmbeddingProvider teiProvider,
            OllamaEmbeddingProvider ollamaProvider,
            OpenAiEmbeddingProvider openAiProvider,
            @Value("${assistant.embedding.provider:stub}") String providerType
    ) {
        // Select the active provider based on configuration; fail fast on unknown values
        this.provider = switch (providerType.toLowerCase()) {
            case "tei" -> teiProvider;
            case "ollama" -> ollamaProvider;
            case "openai" -> openAiProvider;
            case "stub", "local" -> localProvider;
            default -> throw new IllegalArgumentException("Unsupported embedding provider: " + providerType);
        };
    }

    /**
     * Returns the active {@link EmbeddingProvider} for direct embedding calls.
     *
     * @return the provider selected at startup
     */
    public EmbeddingProvider provider() {
        return provider;
    }

    /**
     * Convenience method that returns the active provider's {@link EmbeddingProfile}.
     *
     * @return the profile describing the current model and its output dimensions
     */
    public EmbeddingProfile profile() {
        return provider.profile();
    }
}
