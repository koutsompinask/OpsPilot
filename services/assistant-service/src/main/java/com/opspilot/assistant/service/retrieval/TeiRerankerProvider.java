package com.opspilot.assistant.service.retrieval;

import com.fasterxml.jackson.databind.JsonNode;
import com.opspilot.assistant.exception.BadRequestException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class TeiRerankerProvider implements RerankerProvider {

    private final RestTemplate restTemplate;
    private final RerankerProperties properties;

    public TeiRerankerProvider(
            @Qualifier("rerankerRestTemplate") RestTemplate restTemplate,
            RerankerProperties properties
    ) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @Override
    public String providerName() {
        return "tei";
    }

    @Override
    public String modelName() {
        return properties.getTei().getModel();
    }

    @Override
    public List<RerankResult> rerank(String query, List<String> passages) {
        if (query == null || query.isBlank()) {
            throw new BadRequestException("Reranker query is required");
        }
        if (passages == null || passages.isEmpty()) {
            throw new BadRequestException("Reranker passages are required");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                properties.getTei().getUrl(),
                new HttpEntity<>(new TeiRerankRequest(query, passages, true), headers),
                JsonNode.class
        );

        JsonNode body = response.getBody();
        JsonNode resultsNode = extractResultsNode(body);
        if (resultsNode == null || !resultsNode.isArray()) {
            throw new BadRequestException("Unexpected reranker response from TEI");
        }

        List<RerankResult> results = new ArrayList<>();
        for (JsonNode node : resultsNode) {
            if (node == null || !node.has("index") || (!node.has("score") && !node.has("relevance_score"))) {
                continue;
            }
            double rawScore = node.has("score") ? node.path("score").asDouble() : node.path("relevance_score").asDouble();
            results.add(new RerankResult(node.path("index").asInt(), normalize(rawScore)));
        }

        if (results.isEmpty()) {
            throw new BadRequestException("Unexpected empty reranker response from TEI");
        }

        return results.stream()
                .sorted(Comparator.comparingDouble(RerankResult::score).reversed())
                .toList();
    }

    private JsonNode extractResultsNode(JsonNode body) {
        if (body == null) {
            return null;
        }
        if (body.isArray()) {
            return body;
        }
        if (body.has("results")) {
            return body.get("results");
        }
        if (body.has("data")) {
            return body.get("data");
        }
        return null;
    }

    private double normalize(double rawScore) {
        if (rawScore >= 0.0 && rawScore <= 1.0) {
            return rawScore;
        }
        return 1.0 / (1.0 + Math.exp(-rawScore));
    }

    private record TeiRerankRequest(
            String query,
            List<String> texts,
            boolean truncate
    ) {
    }
}
