package com.opspilot.assistant.service.embedding;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestTemplate;

@Component
public class TeiEmbeddingProvider extends AbstractOpenAiCompatibleEmbeddingProvider {

    private final EmbeddingProperties properties;

    public TeiEmbeddingProvider(@Qualifier("embeddingRestTemplate") RestTemplate embeddingRestTemplate, EmbeddingProperties properties) {
        super(embeddingRestTemplate);
        this.properties = properties;
    }

    @Override
    public EmbeddingProfile profile() {
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
