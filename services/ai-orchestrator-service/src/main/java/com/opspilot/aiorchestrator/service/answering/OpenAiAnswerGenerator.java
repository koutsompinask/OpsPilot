package com.opspilot.aiorchestrator.service.answering;

import com.opspilot.aiorchestrator.repository.RetrievedChunk;
import com.opspilot.aiorchestrator.util.logging.RequestCorrelation;
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
public class OpenAiAnswerGenerator implements AnswerGenerator {

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String model;
    private final String url;

    public OpenAiAnswerGenerator(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${ai.answer.openai.api-key:}") String apiKey,
            @Value("${ai.answer.openai.model:gpt-4o-mini}") String model,
            @Value("${ai.answer.openai.url:https://api.openai.com/v1/chat/completions}") String url
    ) {
        this.restTemplate = restTemplateBuilder.build();
        this.apiKey = apiKey;
        this.model = model;
        this.url = url;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public AnswerGenerationResult generate(String question, List<RetrievedChunk> chunks) {
        if (!isConfigured()) {
            throw new IllegalStateException("OpenAI answer provider is not configured");
        }

        String context = chunks.stream()
                .map(chunk -> "[chunk-" + chunk.chunkIndex() + "] " + chunk.documentName() + ": " + chunk.chunkText())
                .reduce((left, right) -> left + "\n\n" + right)
                .orElse("");

        String userPrompt = "Question:\n" + question + "\n\nContext:\n" + context
                + "\n\nAnswer using only the context. If insufficient context, say so briefly.";

        ChatCompletionRequest request = new ChatCompletionRequest(
                model,
                List.of(
                        new ChatMessage("system", "You are OpsPilot assistant. Give concise grounded answers."),
                        new ChatMessage("user", userPrompt)
                ),
                0.2
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        headers.set(RequestCorrelation.HEADER_NAME, RequestCorrelation.currentRequestId());

        ResponseEntity<ChatCompletionResponse> response = restTemplate.postForEntity(
                url,
                new HttpEntity<>(request, headers),
                ChatCompletionResponse.class
        );

        ChatCompletionResponse body = response.getBody();
        if (body == null || body.choices() == null || body.choices().isEmpty()) {
            throw new IllegalStateException("Unexpected chat response from OpenAI");
        }

        String content = body.choices().get(0).message() == null ? null : body.choices().get(0).message().content();
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("OpenAI chat response did not include answer content");
        }

        return new AnswerGenerationResult(content.trim(), "openai");
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
