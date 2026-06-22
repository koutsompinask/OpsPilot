package com.opspilot.assistant.service.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class GeminiEmbeddingProviderTest {

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/openai/embeddings";

    @Test
    void embedShouldCallGeminiWithBearerAuthAndReturnVectors() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        EmbeddingProperties properties = new EmbeddingProperties();
        properties.getGemini().setApiKey("test-key");
        properties.getGemini().setUrl(GEMINI_URL);
        properties.getGemini().setModel("gemini-embedding-001");
        properties.getGemini().setDimensions(1536);
        GeminiEmbeddingProvider provider = new GeminiEmbeddingProvider(restTemplate, properties);

        server.expect(requestTo(GEMINI_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andRespond(withSuccess(
                        "{\"data\":[{\"embedding\":[0.1,0.2,0.3]}]}",
                        MediaType.APPLICATION_JSON));

        List<List<Double>> result = provider.embed(List.of("hello world"));

        assertThat(result).containsExactly(List.of(0.1, 0.2, 0.3));
        assertThat(provider.profile().id()).isEqualTo("gemini:gemini-embedding-001:1536");
        assertThat(provider.profile().provider()).isEqualTo("gemini");
        server.verify();
    }

    @Test
    void embeddingServiceShouldSelectGeminiProvider() {
        EmbeddingProperties properties = new EmbeddingProperties();
        properties.setProvider("gemini");
        properties.getGemini().setApiKey("test-key");

        RestTemplate restTemplate = new RestTemplate();
        LocalDeterministicEmbeddingProvider stub = new LocalDeterministicEmbeddingProvider(properties);
        TeiEmbeddingProvider tei = new TeiEmbeddingProvider(restTemplate, properties);
        OllamaEmbeddingProvider ollama = new OllamaEmbeddingProvider(restTemplate, properties);
        OpenAiEmbeddingProvider openai = new OpenAiEmbeddingProvider(restTemplate, properties);
        GeminiEmbeddingProvider gemini = new GeminiEmbeddingProvider(restTemplate, properties);

        EmbeddingService service = new EmbeddingService(stub, tei, ollama, openai, gemini, "gemini");

        assertThat(service.profile().provider()).isEqualTo("gemini");
    }
}
