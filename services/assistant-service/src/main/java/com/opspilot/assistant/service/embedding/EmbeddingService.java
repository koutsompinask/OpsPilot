package com.opspilot.assistant.service.embedding;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingService {

    private final EmbeddingProvider provider;

    public EmbeddingService(
            LocalDeterministicEmbeddingProvider localProvider,
            TeiEmbeddingProvider teiProvider,
            OllamaEmbeddingProvider ollamaProvider,
            OpenAiEmbeddingProvider openAiProvider,
            @Value("${assistant.embedding.provider:stub}") String providerType
    ) {
        this.provider = switch (providerType.toLowerCase()) {
            case "tei" -> teiProvider;
            case "ollama" -> ollamaProvider;
            case "openai" -> openAiProvider;
            case "stub", "local" -> localProvider;
            default -> throw new IllegalArgumentException("Unsupported embedding provider: " + providerType);
        };
    }

    public EmbeddingProvider provider() {
        return provider;
    }

    public EmbeddingProfile profile() {
        return provider.profile();
    }
}
