package com.opspilot.knowledgebase.service.embedding;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingService {

    private final EmbeddingProvider provider;

    public EmbeddingService(
            LocalDeterministicEmbeddingProvider localProvider,
            OpenAiEmbeddingProvider openAiProvider,
            @Value("${knowledge.embedding.provider:local}") String providerType
    ) {
        this.provider = "openai".equalsIgnoreCase(providerType) ? openAiProvider : localProvider;
    }

    public EmbeddingProvider provider() {
        return provider;
    }
}
