package com.opspilot.assistant.service.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.opspilot.assistant.repository.DocumentChunkSearchRepository;
import com.opspilot.assistant.repository.RetrievedChunk;
import com.opspilot.assistant.service.embedding.EmbeddingProfile;
import com.opspilot.assistant.service.embedding.EmbeddingProvider;
import com.opspilot.assistant.service.embedding.EmbeddingService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChunkRetrievalServiceTest {

    @Mock
    private DocumentChunkSearchRepository searchRepository;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private EmbeddingProvider embeddingProvider;

    @Mock
    private RerankerService rerankerService;

    @Test
    void retrieveShouldFuseRecallThenReturnRerankedHarborChunkFirst() {
        UUID tenantId = UUID.randomUUID();
        String question = "What time is check-in and check-out?";
        when(embeddingService.profile()).thenReturn(new EmbeddingProfile("tei:test:384", "tei", "test", 384));
        when(embeddingService.provider()).thenReturn(embeddingProvider);
        when(embeddingProvider.embed(List.of(question))).thenReturn(List.of(List.of(0.1, 0.2, 0.3)));

        RetrievedChunk wrongChunk = new RetrievedChunk(UUID.randomUUID(), "hotel-operations.txt", 2, "Late check-out policy", "paragraph",
                "Until 16:00 is charged at 50% of nightly rate. After 16:00 is charged as a full extra night.",
                0.09, null, 0.0, 0.0);
        RetrievedChunk correctChunk = new RetrievedChunk(UUID.randomUUID(), "hotel-operations.txt", 0, "Front Desk Operations", "paragraph",
                "Check-in time starts at 15:00 local time. Check-out time is 11:00 local time.",
                0.16, null, 0.0, 0.0);
        when(searchRepository.searchTopVectorChunks(eq(tenantId), eq("tei:test:384"), anyList(), eq(8)))
                .thenReturn(List.of(wrongChunk, correctChunk));
        when(searchRepository.searchTopLexicalChunks(eq(tenantId), eq("tei:test:384"), eq(question), eq(8)))
                .thenReturn(List.of(correctChunk, wrongChunk));
        when(rerankerService.rerank(eq(question), anyList(), eq(2)))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    List<RetrievedChunk> fused = invocation.getArgument(1, List.class);
                    assertThat(fused).hasSize(2);
                    assertThat(fused.getFirst().chunkIndex()).isEqualTo(2);
                    assertThat(fused.get(1).chunkIndex()).isEqualTo(0);
                    return List.of(
                            fused.get(1).withRerankerScore(0.96),
                            fused.getFirst().withRerankerScore(0.34)
                    );
                });

        ChunkRetrievalService service = new ChunkRetrievalService(searchRepository, embeddingService, rerankerService);
        List<RetrievedChunk> result = service.retrieve(tenantId, question, 2);

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().chunkIndex()).isEqualTo(0);
        assertThat(result.getFirst().chunkText()).contains("Check-in time starts at 15:00");
    }
}
