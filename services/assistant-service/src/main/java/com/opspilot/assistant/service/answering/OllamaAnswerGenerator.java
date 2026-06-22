package com.opspilot.assistant.service.answering;

import com.fasterxml.jackson.databind.ObjectMapper;
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

/**
 * {@link AnswerGenerator} implementation that calls a locally running Ollama instance to
 * produce grounded answers.
 *
 * The generator builds a prompt from the retrieved chunks, posts it to the Ollama
 * {@code /api/generate} endpoint, and parses the {@code "answer"} and
 * {@code "reasoningSummary"} fields from the JSON response. If the model does not return
 * valid JSON, the raw response text is used as the answer.
 *
 * Configured via {@code ai.answer.ollama.*} (model, URL, timeout).
 */
@Component
public class OllamaAnswerGenerator implements AnswerGenerator {

    private final RestTemplate restTemplate;
    private final AnswerProperties properties;
    private final ObjectMapper objectMapper;

    public OllamaAnswerGenerator(
            @Qualifier("answerRestTemplate") RestTemplate answerRestTemplate,
            AnswerProperties properties,
            ObjectMapper objectMapper
    ) {
        this.restTemplate = answerRestTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public AnswerGenerationResult generate(String question, List<RetrievedChunk> chunks) {
        String context = chunks.stream()
                .map(chunk -> "[chunk-" + chunk.chunkIndex() + "] "
                        + chunk.documentName()
                        + " / "
                        + chunk.sectionTitle()
                        + ": "
                        + chunk.chunkText())
                .reduce((left, right) -> left + "\n\n" + right)
                .orElse("");

        String prompt = """
                You are OpsPilot assistant.
                Use only the evidence below.
                Return JSON with keys "answer" and "reasoningSummary".
                Keep both concise. If evidence is insufficient, say so.

                Question:
                %s

                Evidence:
                %s
                """.formatted(question, context);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(RequestCorrelation.HEADER_NAME, RequestCorrelation.currentRequestId());

        ResponseEntity<OllamaResponse> response = restTemplate.postForEntity(
                properties.getOllama().getUrl(),
                new HttpEntity<>(new OllamaRequest(properties.getOllama().getModel(), prompt, false), headers),
                OllamaResponse.class
        );

        OllamaResponse body = response.getBody();
        if (body == null || body.response() == null || body.response().isBlank()) {
            throw new IllegalStateException("Unexpected answer response from Ollama");
        }

        String answer = LlmJson.extractField(objectMapper, body.response(), "answer");
        String reasoningSummary = LlmJson.extractField(objectMapper, body.response(), "reasoningSummary");
        return new AnswerGenerationResult(
                answer == null ? body.response().trim() : answer,
                reasoningSummary == null ? "Generated from grounded evidence returned by the local answer model." : reasoningSummary,
                "ollama",
                "llm-grounded"
        );
    }

    private record OllamaRequest(String model, String prompt, boolean stream) {
    }

    private record OllamaResponse(String response) {
    }
}
