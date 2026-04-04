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

/**
 * Embedding provider backed by a locally running Ollama server.
 *
 * <p>Ollama uses its own native {@code /api/embed} endpoint (not the OpenAI-compatible one),
 * so this provider does not extend {@link AbstractOpenAiCompatibleEmbeddingProvider} and
 * instead implements the full request/response cycle directly. The default model is
 * {@code nomic-embed-text} (768 dimensions).</p>
 */
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
        // Profile id format: "ollama:<model>:<dimensions>" — used for re-indexing detection
        return new EmbeddingProfile(
                "ollama:" + properties.getOllama().getModel() + ":" + properties.getOllama().getDimensions(),
                "ollama",
                properties.getOllama().getModel(),
                properties.getOllama().getDimensions()
        );
    }

    /**
     * Sends a batch of texts to Ollama's native embed endpoint and returns the vectors.
     *
     * <p>Ollama's {@code /api/embed} returns an {@code embeddings} array (not {@code data}),
     * which is why this provider cannot reuse the base class. Response length is validated
     * to ensure no silent mismatches.</p>
     *
     * @param inputs texts to embed; must be non-null and non-empty
     * @return one embedding vector per input, in the same order
     */
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
        // Validate that the embeddings array length matches the input count
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
