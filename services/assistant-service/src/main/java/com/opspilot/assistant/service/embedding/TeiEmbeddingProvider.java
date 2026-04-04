package com.opspilot.assistant.service.embedding;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestTemplate;

/**
 * Embedding provider backed by a Hugging Face Text Embeddings Inference (TEI) server
 * running in OpenAI-compatible mode ({@code /v1/embeddings}).
 *
 * <p>TEI does not require an authorization header; authentication is handled at the
 * network level. The default model is {@code BAAI/bge-small-en-v1.5} (384 dimensions)
 * but can be overridden via {@code assistant.embedding.tei.model}.</p>
 */
@Component
public class TeiEmbeddingProvider extends AbstractOpenAiCompatibleEmbeddingProvider {

    private final EmbeddingProperties properties;

    public TeiEmbeddingProvider(@Qualifier("embeddingRestTemplate") RestTemplate embeddingRestTemplate, EmbeddingProperties properties) {
        super(embeddingRestTemplate);
        this.properties = properties;
    }

    @Override
    public EmbeddingProfile profile() {
        // Profile id format: "tei:<model>:<dimensions>" — used for re-indexing detection
        return new EmbeddingProfile(
                "tei:" + properties.getTei().getModel() + ":" + properties.getTei().getDimensions(),
                "tei",
                properties.getTei().getModel(),
                properties.getTei().getDimensions()
        );
    }

    @Override
    protected String providerName() {
        return "TEI";
    }

    @Override
    protected String modelName() {
        return properties.getTei().getModel();
    }

    @Override
    protected String endpointUrl() {
        return properties.getTei().getUrl();
    }
}
