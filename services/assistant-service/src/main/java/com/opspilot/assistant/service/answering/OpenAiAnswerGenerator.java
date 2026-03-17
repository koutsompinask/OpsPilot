package com.opspilot.assistant.service.answering;

import com.opspilot.assistant.repository.RetrievedChunk;
import com.opspilot.assistant.util.logging.RequestCorrelation;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class OpenAiAnswerGenerator implements AnswerGenerator {

    private final RestTemplate restTemplate;
    private final AnswerProperties properties;

    public OpenAiAnswerGenerator(@Qualifier("answerRestTemplate") RestTemplate answerRestTemplate, AnswerProperties properties) {
        this.restTemplate = answerRestTemplate;
        this.properties = properties;
    }

    public boolean isConfigured() {
        String apiKey = properties.getOpenai().getApiKey();
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public AnswerGenerationResult generate(String question, List<RetrievedChunk> chunks) {
        if (!isConfigured()) {
            throw new IllegalStateException("OpenAI answer provider is not configured");
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
                properties.getOpenai().getModel(),
                List.of(
                        new ChatMessage("system", "You are OpsPilot assistant. Provide concise grounded answers in JSON."),
                        new ChatMessage("user", userPrompt)
                ),
                0.1
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getOpenai().getApiKey());
        headers.set(RequestCorrelation.HEADER_NAME, RequestCorrelation.currentRequestId());

        ResponseEntity<ChatCompletionResponse> response = restTemplate.postForEntity(
                properties.getOpenai().getUrl(),
                new HttpEntity<>(request, headers),
                ChatCompletionResponse.class
        );

        ChatCompletionResponse body = response.getBody();
        if (body == null || body.choices() == null || body.choices().isEmpty()) {
            throw new IllegalStateException("Unexpected chat response from OpenAI");
        }

        String content = body.choices().getFirst().message() == null ? null : body.choices().getFirst().message().content();
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("OpenAI chat response did not include answer content");
        }

        String answer = extractJsonField(content, "answer");
        String reasoningSummary = extractJsonField(content, "reasoningSummary");
        return new AnswerGenerationResult(
                answer == null ? content.trim() : answer,
                reasoningSummary == null ? "Generated from grounded evidence returned by the chat model." : reasoningSummary,
                "openai",
                "llm-grounded"
        );
    }

    private String extractJsonField(String content, String field) {
        String marker = "\"" + field + "\"";
        int start = content.indexOf(marker);
        if (start < 0) {
            return null;
        }
        int colon = content.indexOf(':', start);
        int firstQuote = content.indexOf('"', colon + 1);
        int secondQuote = content.indexOf('"', firstQuote + 1);
        if (colon < 0 || firstQuote < 0 || secondQuote < 0) {
            return null;
        }
        return content.substring(firstQuote + 1, secondQuote).trim();
    }

    private record ChatCompletionRequest(String model, List<ChatMessage> messages, double temperature) {
    }

    private record ChatMessage(String role, String content) {
    }

    private record ChatCompletionResponse(List<Choice> choices) {
        private record Choice(ChatMessage message) {
        }
    }
}
