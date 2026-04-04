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

/**
 * Reranker provider backed by a Hugging Face Text Embeddings Inference (TEI) server.
 *
 * <p>TEI's rerank endpoint returns an array of {@code {index, score}} objects (or
 * {@code {index, relevance_score}} depending on model version). This provider handles
 * both field names and applies sigmoid normalisation to scores that fall outside
 * {@code [0, 1]}, so all scores returned are guaranteed to be in that range regardless
 * of whether the model output is a raw logit or a probability.</p>
 */
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

    /**
     * Sends the query and passages to the TEI rerank endpoint and returns scored results.
     *
     * <p>The {@code truncate=true} flag in the request body asks TEI to silently truncate
     * passages that exceed the model's token limit rather than returning an error, keeping
     * behaviour consistent even for long chunks.</p>
     *
     * @param query    the user's question; must be non-blank
     * @param passages the candidate passages to score; must be non-empty
     * @return results sorted by descending relevance score
     */
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

        // truncate=true prevents token-limit errors on long passages
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                properties.getTei().getUrl(),
                new HttpEntity<>(new TeiRerankRequest(query, passages, true), headers),
                JsonNode.class
        );

        JsonNode body = response.getBody();
        // TEI can return either a top-level array or an object with "results"/"data" key
        JsonNode resultsNode = extractResultsNode(body);
        if (resultsNode == null || !resultsNode.isArray()) {
            throw new BadRequestException("Unexpected reranker response from TEI");
        }

        List<RerankResult> results = new ArrayList<>();
        for (JsonNode node : resultsNode) {
            // Tolerate both "score" (newer TEI) and "relevance_score" (older TEI) field names
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
        // Handle flat array, {"results": [...]} and {"data": [...]} response shapes
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
        // If score is already a probability in [0, 1], return as-is
        if (rawScore >= 0.0 && rawScore <= 1.0) {
            return rawScore;
        }
        // Otherwise apply sigmoid to convert raw logits into [0, 1]
        return 1.0 / (1.0 + Math.exp(-rawScore));
    }

    private record TeiRerankRequest(
            String query,
            List<String> texts,
            boolean truncate
    ) {
    }
}
