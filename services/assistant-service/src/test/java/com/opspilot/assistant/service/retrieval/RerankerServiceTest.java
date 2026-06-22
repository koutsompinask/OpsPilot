package com.opspilot.assistant.service.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.opspilot.assistant.repository.RetrievedChunk;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RerankerServiceTest {

    @Mock
    private TeiRerankerProvider teiRerankerProvider;

    @Mock
    private GeminiReranker geminiReranker;

    @Mock
    private HeuristicReranker heuristicReranker;

    @Test
    void rerankShouldUseProviderScoresWhenAvailable() {
        RerankerProperties properties = new RerankerProperties();
        properties.setEnabled(true);
        properties.setCandidateLimit(12);
        when(teiRerankerProvider.rerank(eq("What time is check-in and check-out?"), anyList()))
                .thenReturn(List.of(new RerankResult(1, 0.97), new RerankResult(0, 0.32)));
        when(teiRerankerProvider.providerName()).thenReturn("tei");
        when(teiRerankerProvider.modelName()).thenReturn("BAAI/bge-reranker-base");
        RerankerService service = new RerankerService(properties, teiRerankerProvider, geminiReranker, heuristicReranker);

        List<RetrievedChunk> result = service.rerank("What time is check-in and check-out?", candidates(), 2);

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().chunkIndex()).isEqualTo(1);
        assertThat(result.getFirst().rerankerScore()).isEqualTo(0.97);
    }

    @Test
    void rerankShouldFallbackToHeuristicWhenProviderFails() {
        RerankerProperties properties = new RerankerProperties();
        properties.setEnabled(true);
        when(teiRerankerProvider.providerName()).thenReturn("tei");
        when(teiRerankerProvider.modelName()).thenReturn("BAAI/bge-reranker-base");
        doThrow(new IllegalStateException("down")).when(teiRerankerProvider).rerank(eq("What time is check-in and check-out?"), anyList());
        when(heuristicReranker.rerank(eq("What time is check-in and check-out?"), anyList()))
                .thenReturn(List.of(new RerankResult(0, 0.91), new RerankResult(1, 0.22)));
        RerankerService service = new RerankerService(properties, teiRerankerProvider, geminiReranker, heuristicReranker);

        List<RetrievedChunk> result = service.rerank("What time is check-in and check-out?", candidates(), 2);

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().chunkIndex()).isEqualTo(0);
        assertThat(result.getFirst().rerankerScore()).isEqualTo(0.91);
    }

    @Test
    void rerankShouldSelectGeminiProviderWhenConfigured() {
        RerankerProperties properties = new RerankerProperties();
        properties.setEnabled(true);
        properties.setProvider("gemini");
        when(geminiReranker.rerank(eq("What time is check-in and check-out?"), anyList()))
                .thenReturn(List.of(new RerankResult(0, 0.93), new RerankResult(1, 0.45)));
        when(geminiReranker.providerName()).thenReturn("gemini");
        when(geminiReranker.modelName()).thenReturn("gemini-2.5-flash");
        RerankerService service = new RerankerService(properties, teiRerankerProvider, geminiReranker, heuristicReranker);

        List<RetrievedChunk> result = service.rerank("What time is check-in and check-out?", candidates(), 2);

        assertThat(result.getFirst().chunkIndex()).isEqualTo(0);
        assertThat(result.getFirst().rerankerScore()).isEqualTo(0.93);
    }

    private List<RetrievedChunk> candidates() {
        return List.of(
                new RetrievedChunk(UUID.randomUUID(), "hotel-operations.txt", 0, "Front Desk Operations", "paragraph",
                        "Check-in time starts at 15:00 local time. Check-out time is 11:00 local time.",
                        0.11, 0.88, 0.032, 0.0),
                new RetrievedChunk(UUID.randomUUID(), "hotel-operations.txt", 1, "Late check-out policy", "paragraph",
                        "Until 16:00 is charged at 50% of nightly rate.",
                        0.18, 0.74, 0.027, 0.0)
        );
    }
}
