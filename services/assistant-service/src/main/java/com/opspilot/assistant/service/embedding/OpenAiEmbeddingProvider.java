package com.opspilot.assistant.service.embedding;

import com.opspilot.assistant.exception.BadRequestException;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Embedding provider backed by the OpenAI embeddings API.
 *
 * <p>Requires a non-blank API key in {@code assistant.embedding.openai.api-key}; startup
 * validation will fail fast if the key is missing and this provider is active. The default
 * model is {@code text-embedding-3-small} (1536 dimensions).</p>
 */
@Component
public class OpenAiEmbeddingProvider extends AbstractOpenAiCompatibleEmbeddingProvider {

    private final EmbeddingProperties properties;

    public OpenAiEmbeddingProvider(
            @Qualifier("embeddingRestTemplate") RestTemplate embeddingRestTemplate,
            EmbeddingProperties properties
    ) {
        super(embeddingRestTemplate);
        this.properties = properties;
    }

    @Override
    public EmbeddingProfile profile() {
        // Profile id format: "openai:<model>:<dimensions>" — used for re-indexing detection
        return new EmbeddingProfile(
                "openai:" + properties.getOpenai().getModel() + ":" + properties.getOpenai().getDimensions(),
                "openai",
                properties.getOpenai().getModel(),
                properties.getOpenai().getDimensions()
        );
    }

    @Override
    protected void applyAuthorization(HttpHeaders headers) {
        if (properties.getOpenai().getApiKey() == null || properties.getOpenai().getApiKey().isBlank()) {
            throw new BadRequestException("OpenAI embedding provider requires API key");
        }
        headers.setBearerAuth(properties.getOpenai().getApiKey());
    }

    @Override
    protected String providerName() {
        return "OpenAI";
    }

    @Override
    protected String modelName() {
        return properties.getOpenai().getModel();
    }

    @Override
    protected String endpointUrl() {
        return properties.getOpenai().getUrl();
    }
}
