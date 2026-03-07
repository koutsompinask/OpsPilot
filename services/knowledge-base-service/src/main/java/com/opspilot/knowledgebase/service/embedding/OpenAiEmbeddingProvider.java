package com.opspilot.knowledgebase.service.embedding;

import com.opspilot.knowledgebase.exception.BadRequestException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class OpenAiEmbeddingProvider implements EmbeddingProvider {

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String model;
    private final String url;

    public OpenAiEmbeddingProvider(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${knowledge.embedding.openai.api-key:}") String apiKey,
            @Value("${knowledge.embedding.openai.model:text-embedding-3-small}") String model,
            @Value("${knowledge.embedding.openai.url:https://api.openai.com/v1/embeddings}") String url
    ) {
        this.restTemplate = restTemplateBuilder.build();
        this.apiKey = apiKey;
        this.model = model;
        this.url = url;
    }

    @Override
    public List<List<Double>> embed(List<String> inputs) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BadRequestException("OpenAI embedding provider requires API key");
        }

        OpenAiEmbeddingRequest request = new OpenAiEmbeddingRequest(model, inputs);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        ResponseEntity<OpenAiEmbeddingResponse> response = restTemplate.postForEntity(
                url,
                new HttpEntity<>(request, headers),
                OpenAiEmbeddingResponse.class
        );

        OpenAiEmbeddingResponse body = response.getBody();
        if (body == null || body.data() == null || body.data().size() != inputs.size()) {
            throw new BadRequestException("Unexpected embedding response from OpenAI");
        }

        return body.data().stream().map(OpenAiEmbeddingResponse.EmbeddingData::embedding).toList();
    }

    private record OpenAiEmbeddingRequest(String model, List<String> input) {
    }

    private record OpenAiEmbeddingResponse(List<EmbeddingData> data) {
        private record EmbeddingData(List<Double> embedding) {
        }
    }
}
