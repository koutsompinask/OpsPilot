package com.opspilot.assistant.service.retrieval;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * {@link RerankerProvider} that uses Gemini Flash as a relevance scorer.
 *
 * <p>Sends the query and numbered passage list to the Gemini OpenAI-compatible chat endpoint
 * with {@code response_format: json_object}. The model is instructed to return
 * {@code {"scores":[{"index":N,"score":0..1},...]}}, which is then parsed into
 * {@link RerankResult}s. Scores already in [0, 1] are used as-is; values outside that range
 * are sigmoid-normalised.</p>
 */
@Component
public class GeminiReranker implements RerankerProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiReranker.class);

    private final RestTemplate restTemplate;
    private final RerankerProperties properties;
    private final ObjectMapper objectMapper;

    public GeminiReranker(
            @Qualifier("rerankerRestTemplate") RestTemplate rerankerRestTemplate,
            RerankerProperties properties,
            ObjectMapper objectMapper
    ) {
        this.restTemplate = rerankerRestTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public String providerName() {
        return "gemini";
    }

    @Override
    public String modelName() {
        return properties.getGemini().getModel();
    }

    @Override
    public List<RerankResult> rerank(String query, List<String> passages) {
        StringBuilder passageBlock = new StringBuilder();
        for (int i = 0; i < passages.size(); i++) {
            passageBlock.append("[").append(i).append("] ").append(passages.get(i)).append("\n");
        }

        String userMessage = """
                Query: %s

                Passages:
                %s
                Return JSON with key "scores" containing an array of objects, each with "index" (0-based integer) \
                and "score" (float 0.0-1.0, higher means more relevant to the query). \
                Include an entry for every passage index.
                """.formatted(query, passageBlock);

        ChatCompletionRequest request = new ChatCompletionRequest(
                properties.getGemini().getModel(),
                List.of(
                        new ChatMessage("system",
                                "You are a relevance scoring system. Score passages by relevance to the query. " +
                                "Return only valid JSON."),
                        new ChatMessage("user", userMessage)
                ),
                0.0,
                Map.of("type", "json_object")
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String apiKey = properties.getGemini().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Gemini reranker requires an API key");
        }
        headers.setBearerAuth(apiKey);

        ResponseEntity<String> response = restTemplate.postForEntity(
                properties.getGemini().getUrl(),
                new HttpEntity<>(request, headers),
                String.class
        );

        return parseScores(response.getBody(), passages.size());
    }

    private List<RerankResult> parseScores(String body, int passageCount) {
        if (body == null || body.isBlank()) {
            log.warn("gemini_reranker_empty_response");
            return fallbackScores(passageCount);
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            // Navigate through choices[0].message.content if wrapped in chat response
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            String json = content.isMissingNode() ? body : content.asText();
            JsonNode parsed = objectMapper.readTree(json);
            JsonNode scores = parsed.get("scores");
            if (scores == null || !scores.isArray()) {
                log.warn("gemini_reranker_unexpected_shape");
                return fallbackScores(passageCount);
            }
            List<RerankResult> results = new ArrayList<>(scores.size());
            for (JsonNode entry : scores) {
                int index = entry.path("index").asInt(-1);
                double score = entry.path("score").asDouble(0.0);
                if (index >= 0 && index < passageCount) {
                    results.add(new RerankResult(index, normalise(score)));
                }
            }
            return results;
        } catch (Exception ex) {
            log.warn("gemini_reranker_parse_failed reason={}", ex.getMessage());
            return fallbackScores(passageCount);
        }
    }

    private List<RerankResult> fallbackScores(int count) {
        List<RerankResult> results = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            results.add(new RerankResult(i, 0.5));
        }
        return results;
    }

    private double normalise(double score) {
        if (score >= 0.0 && score <= 1.0) {
            return score;
        }
        return 1.0 / (1.0 + Math.exp(-score));
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record ChatCompletionRequest(
            String model,
            List<ChatMessage> messages,
            double temperature,
            @JsonInclude(JsonInclude.Include.NON_NULL) Map<String, String> response_format
    ) {
    }

    private record ChatMessage(String role, String content) {
    }
}
