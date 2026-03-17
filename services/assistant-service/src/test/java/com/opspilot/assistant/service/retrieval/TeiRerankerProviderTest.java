package com.opspilot.assistant.service.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class TeiRerankerProviderTest {

    @Test
    void rerankShouldParseResultsObject() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        RerankerProperties properties = new RerankerProperties();
        properties.getTei().setUrl("http://localhost:8092/rerank");
        properties.getTei().setModel("BAAI/bge-reranker-base");
        TeiRerankerProvider provider = new TeiRerankerProvider(restTemplate, properties);

        server.expect(requestTo("http://localhost:8092/rerank"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"results":[{"index":1,"score":2.5},{"index":0,"score":0.5}]}
                        """, MediaType.APPLICATION_JSON));

        List<RerankResult> result = provider.rerank("check-in", List.of("a", "b"));

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().index()).isEqualTo(1);
        assertThat(result.getFirst().score()).isCloseTo(0.9241418, within(0.000001));
        assertThat(result.get(1).index()).isEqualTo(0);
        assertThat(result.get(1).score()).isEqualTo(0.5);
        server.verify();
    }
}
