package com.opspilot.assistant.service.embedding;

import com.opspilot.assistant.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Embedding provider backed by the Gemini API (AI Studio) using its OpenAI-compatible
 * {@code /v1beta/openai/embeddings} endpoint.
 *
 * <p>Requires a non-blank API key in {@code assistant.embedding.gemini.api-key}.
 * The default model is {@code gemini-embedding-001} with 1536 output dimensions
 * (truncated from the native 3072; configurable via {@code assistant.embedding.gemini.dimensions}).</p>
 */
@Component
public class GeminiEmbeddingProvider extends AbstractOpenAiCompatibleEmbeddingProvider {

    private final EmbeddingProperties properties;

    public GeminiEmbeddingProvider(
            @Qualifier("embeddingRestTemplate") RestTemplate embeddingRestTemplate,
            EmbeddingProperties properties
    ) {
        super(embeddingRestTemplate);
        this.properties = properties;
    }

    @Override
    public EmbeddingProfile profile() {
        return new EmbeddingProfile(
                "gemini:" + properties.getGemini().getModel() + ":" + properties.getGemini().getDimensions(),
                "gemini",
                properties.getGemini().getModel(),
                properties.getGemini().getDimensions()
        );
    }

    @Override
    protected void applyAuthorization(HttpHeaders headers) {
        String apiKey = properties.getGemini().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new BadRequestException("Gemini embedding provider requires an API key");
        }
        headers.setBearerAuth(apiKey);
    }

    @Override
    protected String providerName() {
        return "Gemini";
    }

    @Override
    protected String modelName() {
        return properties.getGemini().getModel();
    }

    @Override
    protected String endpointUrl() {
        return properties.getGemini().getUrl();
    }

    @Override
    protected Integer requestedDimensions() {
        return properties.getGemini().getDimensions();
    }
}
