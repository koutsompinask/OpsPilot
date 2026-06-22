package com.opspilot.assistant.service.answering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspilot.assistant.repository.RetrievedChunk;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class GeminiAnswerGeneratorTest {

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void generateShouldParseAnswerFromGeminiResponse() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        AnswerProperties properties = new AnswerProperties();
        properties.getGemini().setApiKey("test-key");
        properties.getGemini().setUrl(GEMINI_URL);
        properties.getGemini().setModel("gemini-2.5-flash");
        GeminiAnswerGenerator generator = new GeminiAnswerGenerator(restTemplate, properties, objectMapper);

        String chatResponse = """
                {"choices":[{"message":{"role":"assistant","content":"{\\"answer\\":\\"Check-in is at 15:00.\\",\\"reasoningSummary\\":\\"From front desk doc.\\"}"}}]}
                """;
        server.expect(requestTo(GEMINI_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andRespond(withSuccess(chatResponse, MediaType.APPLICATION_JSON));

        AnswerGenerationResult result = generator.generate("When is check-in?", List.of(chunk()));

        assertThat(result.answer()).isEqualTo("Check-in is at 15:00.");
        assertThat(result.reasoningSummary()).isEqualTo("From front desk doc.");
        assertThat(result.provider()).isEqualTo("gemini");
        assertThat(result.answerMode()).isEqualTo("llm-grounded");
        server.verify();
    }

    @Test
    void isConfiguredReturnsFalseWhenNoApiKey() {
        AnswerProperties properties = new AnswerProperties();
        GeminiAnswerGenerator generator = new GeminiAnswerGenerator(new RestTemplate(), properties, objectMapper);
        assertThat(generator.isConfigured()).isFalse();
    }

    @Test
    void generateThrowsWhenNotConfigured() {
        AnswerProperties properties = new AnswerProperties();
        GeminiAnswerGenerator generator = new GeminiAnswerGenerator(new RestTemplate(), properties, objectMapper);
        assertThatThrownBy(() -> generator.generate("q", List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not configured");
    }

    private RetrievedChunk chunk() {
        return new RetrievedChunk(UUID.randomUUID(), "hotel.txt", 0, "Front Desk", "paragraph",
                "Check-in is at 15:00.", 0.1, 0.9, 0.05, 0.88);
    }
}
