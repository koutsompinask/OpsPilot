package com.opspilot.assistant.service.embedding;

import com.opspilot.assistant.exception.BadRequestException;
import java.util.List;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

abstract class AbstractOpenAiCompatibleEmbeddingProvider implements EmbeddingProvider {

    private final RestTemplate restTemplate;

    protected AbstractOpenAiCompatibleEmbeddingProvider(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public List<List<Double>> embed(List<String> inputs) {
        validateInputs(inputs);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        applyAuthorization(headers);

        ResponseEntity<OpenAiEmbeddingResponse> response = restTemplate.postForEntity(
                endpointUrl(),
                new HttpEntity<>(new OpenAiEmbeddingRequest(modelName(), inputs), headers),
                OpenAiEmbeddingResponse.class
        );

        OpenAiEmbeddingResponse body = response.getBody();
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

    protected void applyAuthorization(HttpHeaders headers) {
    }

    protected abstract String providerName();

    protected abstract String modelName();

    protected abstract String endpointUrl();

    protected record OpenAiEmbeddingRequest(String model, List<String> input) {
    }

    protected record OpenAiEmbeddingResponse(List<EmbeddingData> data) {
        protected record EmbeddingData(List<Double> embedding) {
        }
    }
}
