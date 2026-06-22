package com.opspilot.assistant.service.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class GeminiRerankerTest {

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void rerankShouldParseScoredPassagesFromChatCompletionResponse() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        RerankerProperties properties = new RerankerProperties();
        properties.getGemini().setApiKey("test-key");
        properties.getGemini().setUrl(GEMINI_URL);
        properties.getGemini().setModel("gemini-2.5-flash");
        GeminiReranker reranker = new GeminiReranker(restTemplate, properties, objectMapper);

        // The response wraps the scores JSON inside choices[0].message.content
        String chatResponse = """
                {"choices":[{"message":{"role":"assistant","content":"{\\"scores\\":[{\\"index\\":0,\\"score\\":0.95},{\\"index\\":1,\\"score\\":0.3}]}"}}]}
                """;
        server.expect(requestTo(GEMINI_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andRespond(withSuccess(chatResponse, MediaType.APPLICATION_JSON));

        List<RerankResult> results = reranker.rerank(
                "When is check-in?",
                List.of("Check-in is at 15:00.", "Breakfast is at 07:00."));

        assertThat(results).hasSize(2);
        RerankResult top = results.stream().max(java.util.Comparator.comparingDouble(RerankResult::score)).orElseThrow();
        assertThat(top.index()).isEqualTo(0);
        assertThat(top.score()).isEqualTo(0.95);
        server.verify();
    }

    @Test
    void rerankShouldReturnFallbackScoresOnEmptyBody() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        RerankerProperties properties = new RerankerProperties();
        properties.getGemini().setApiKey("test-key");
        properties.getGemini().setUrl(GEMINI_URL);
        GeminiReranker reranker = new GeminiReranker(restTemplate, properties, objectMapper);

        server.expect(requestTo(GEMINI_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        List<RerankResult> results = reranker.rerank("q", List.of("a", "b"));

        assertThat(results).hasSize(2);
        results.forEach(r -> assertThat(r.score()).isEqualTo(0.5));
    }

    @Test
    void providerNameAndModelNameAreCorrect() {
        RerankerProperties properties = new RerankerProperties();
        properties.getGemini().setModel("gemini-2.5-flash");
        GeminiReranker reranker = new GeminiReranker(new RestTemplate(), properties, objectMapper);
        assertThat(reranker.providerName()).isEqualTo("gemini");
        assertThat(reranker.modelName()).isEqualTo("gemini-2.5-flash");
    }
}
