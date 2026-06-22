package com.opspilot.assistant.service.embedding;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.opspilot.assistant.exception.BadRequestException;
import java.util.List;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * Base class for embedding providers that speak the OpenAI embeddings API contract
 * ({@code POST /v1/embeddings} with a {@code model} + {@code input} JSON body).
 *
 * <p>Both {@link TeiEmbeddingProvider} (TEI's OpenAI-compatible endpoint) and
 * {@link OpenAiEmbeddingProvider} extend this class. Subclasses supply the target URL,
 * model name, and optional authorization header via abstract methods; the common
 * HTTP request/response plumbing lives here.</p>
 */
abstract class AbstractOpenAiCompatibleEmbeddingProvider implements EmbeddingProvider {

    private final RestTemplate restTemplate;

    protected AbstractOpenAiCompatibleEmbeddingProvider(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Sends a batch of texts to the provider's embeddings endpoint and returns the resulting vectors.
     *
     * <p>Response validation checks that the returned {@code data} array has exactly as many entries
     * as the input list, preventing silent mismatches from partial responses.</p>
     *
     * @param inputs texts to embed; must be non-null and non-empty
     * @return one embedding vector per input, in the same order
     * @throws com.opspilot.assistant.exception.BadRequestException if the response is null or has an unexpected shape
     */
    @Override
    public List<List<Double>> embed(List<String> inputs) {
        validateInputs(inputs);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        applyAuthorization(headers);

        ResponseEntity<OpenAiEmbeddingResponse> response = restTemplate.postForEntity(
                endpointUrl(),
                new HttpEntity<>(new OpenAiEmbeddingRequest(modelName(), inputs, requestedDimensions()), headers),
                OpenAiEmbeddingResponse.class
        );

        OpenAiEmbeddingResponse body = response.getBody();
        // Validate that the data array length exactly matches the input list
        if (body == null || body.data() == null || body.data().size() != inputs.size()) {
            throw new BadRequestException("Unexpected embedding response from " + providerName());
        }

        return body.data().stream().map(OpenAiEmbeddingResponse.EmbeddingData::embedding).toList();
    }

    protected void validateInputs(List<String> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            throw new BadRequestException("Embedding inputs are required");
        }
    }

    /**
     * Hook for subclasses to attach authorization headers (e.g. a Bearer token).
     * Default implementation is a no-op (used by providers that do not require auth).
     *
     * @param headers the mutable request headers to decorate
     */
    protected void applyAuthorization(HttpHeaders headers) {
    }

    /** @return the human-readable provider name used in error messages */
    protected abstract String providerName();

    /** @return the model name to pass in the {@code model} request field */
    protected abstract String modelName();

    /** @return the absolute URL of the embeddings endpoint */
    protected abstract String endpointUrl();

    /**
     * Optional output dimension to request from the provider.
     * Returns {@code null} by default; override in providers that support it (e.g. Gemini).
     */
    protected Integer requestedDimensions() {
        return null;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected record OpenAiEmbeddingRequest(String model, List<String> input, Integer dimensions) {
    }

    protected record OpenAiEmbeddingResponse(List<EmbeddingData> data) {
        protected record EmbeddingData(List<Double> embedding) {
        }
    }
}
