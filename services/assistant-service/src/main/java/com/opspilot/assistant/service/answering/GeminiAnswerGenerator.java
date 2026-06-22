package com.opspilot.assistant.service.answering;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspilot.assistant.repository.RetrievedChunk;
import com.opspilot.assistant.util.logging.RequestCorrelation;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * {@link AnswerGenerator} backed by the Gemini API (AI Studio) using its OpenAI-compatible
 * {@code /chat/completions} endpoint.
 *
 * <p>Uses {@code response_format: json_object} to guarantee structured output, and parses
 * the {@code "answer"} and {@code "reasoningSummary"} fields with Jackson via {@link LlmJson}.
 * Requires a non-blank API key in {@code ai.answer.gemini.api-key}.</p>
 */
@Component
public class GeminiAnswerGenerator implements AnswerGenerator {

    private final RestTemplate restTemplate;
    private final AnswerProperties properties;
    private final ObjectMapper objectMapper;

    public GeminiAnswerGenerator(
            @Qualifier("answerRestTemplate") RestTemplate answerRestTemplate,
            AnswerProperties properties,
            ObjectMapper objectMapper
    ) {
        this.restTemplate = answerRestTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * Returns {@code true} if a Gemini API key has been configured.
     */
    public boolean isConfigured() {
        String apiKey = properties.getGemini().getApiKey();
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public AnswerGenerationResult generate(String question, List<RetrievedChunk> chunks) {
        if (!isConfigured()) {
            throw new IllegalStateException("Gemini answer provider is not configured");
        }

        String context = chunks.stream()
                .map(chunk -> "[chunk-" + chunk.chunkIndex() + "] "
                        + chunk.documentName()
                        + " / "
                        + chunk.sectionTitle()
                        + ": "
                        + chunk.chunkText())
                .reduce((left, right) -> left + "\n\n" + right)
                .orElse("");

        String userPrompt = """
                Question:
                %s

                Evidence:
                %s

                Return JSON with keys "answer" and "reasoningSummary".
                Use only the evidence. If evidence is insufficient, say that briefly in both fields.
                """.formatted(question, context);

        ChatCompletionRequest request = new ChatCompletionRequest(
                properties.getGemini().getModel(),
                List.of(
                        new ChatMessage("system", "You are OpsPilot assistant. Provide concise grounded answers in JSON."),
                        new ChatMessage("user", userPrompt)
                ),
                0.1,
                Map.of("type", "json_object")
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getGemini().getApiKey());
        headers.set(RequestCorrelation.HEADER_NAME, RequestCorrelation.currentRequestId());

        ResponseEntity<ChatCompletionResponse> response = restTemplate.postForEntity(
                properties.getGemini().getUrl(),
                new HttpEntity<>(request, headers),
                ChatCompletionResponse.class
        );

        ChatCompletionResponse body = response.getBody();
        if (body == null || body.choices() == null || body.choices().isEmpty()) {
            throw new IllegalStateException("Unexpected chat response from Gemini");
        }

        String content = body.choices().getFirst().message() == null ? null
                : body.choices().getFirst().message().content();
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("Gemini chat response did not include answer content");
        }

        String answer = LlmJson.extractField(objectMapper, content, "answer");
        String reasoningSummary = LlmJson.extractField(objectMapper, content, "reasoningSummary");
        return new AnswerGenerationResult(
                answer == null ? content.trim() : answer,
                reasoningSummary == null ? "Generated from grounded evidence returned by the Gemini model." : reasoningSummary,
                "gemini",
                "llm-grounded"
        );
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

    private record ChatCompletionResponse(List<Choice> choices) {
        private record Choice(ChatMessage message) {
        }
    }
}
