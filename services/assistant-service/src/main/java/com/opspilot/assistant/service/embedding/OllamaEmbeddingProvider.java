package com.opspilot.assistant.service.embedding;

import com.opspilot.assistant.exception.BadRequestException;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class OllamaEmbeddingProvider implements EmbeddingProvider {

    private final RestTemplate restTemplate;
    private final EmbeddingProperties properties;

    public OllamaEmbeddingProvider(@Qualifier("embeddingRestTemplate") RestTemplate embeddingRestTemplate, EmbeddingProperties properties) {
        this.restTemplate = embeddingRestTemplate;
        this.properties = properties;
    }

    @Override
    public EmbeddingProfile profile() {
        return new EmbeddingProfile(
                "ollama:" + properties.getOllama().getModel() + ":" + properties.getOllama().getDimensions(),
                "ollama",
                properties.getOllama().getModel(),
                properties.getOllama().getDimensions()
        );
    }

    @Override
    public List<List<Double>> embed(List<String> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            throw new BadRequestException("Embedding inputs are required");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<OllamaEmbeddingResponse> response = restTemplate.postForEntity(
                properties.getOllama().getUrl(),
                new HttpEntity<>(new OllamaEmbeddingRequest(properties.getOllama().getModel(), inputs), headers),
                OllamaEmbeddingResponse.class
        );

        OllamaEmbeddingResponse body = response.getBody();
        if (body == null || body.embeddings() == null || body.embeddings().size() != inputs.size()) {
            throw new BadRequestException("Unexpected embedding response from Ollama");
        }

        return body.embeddings();
    }

    private record OllamaEmbeddingRequest(String model, List<String> input) {
    }

    private record OllamaEmbeddingResponse(List<List<Double>> embeddings) {
    }
}
