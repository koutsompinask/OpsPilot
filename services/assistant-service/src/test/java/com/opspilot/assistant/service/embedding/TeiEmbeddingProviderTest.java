package com.opspilot.assistant.service.embedding;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TeiEmbeddingProviderTest {

    @Test
    void embedShouldCallOpenAiCompatibleEndpoint() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        EmbeddingProperties properties = new EmbeddingProperties();
        properties.getTei().setUrl("http://localhost:8091/v1/embeddings");
        properties.getTei().setModel("BAAI/bge-small-en-v1.5");
        properties.getTei().setDimensions(384);
        TeiEmbeddingProvider provider = new TeiEmbeddingProvider(restTemplate, properties);

        server.expect(requestTo("http://localhost:8091/v1/embeddings"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"data":[{"embedding":[0.1,0.2,0.3]}]}
                        """, MediaType.APPLICATION_JSON));

        List<List<Double>> result = provider.embed(List.of("hello"));

        assertThat(result).containsExactly(List.of(0.1, 0.2, 0.3));
        assertThat(provider.profile().id()).isEqualTo("tei:BAAI/bge-small-en-v1.5:384");
        server.verify();
    }
}
