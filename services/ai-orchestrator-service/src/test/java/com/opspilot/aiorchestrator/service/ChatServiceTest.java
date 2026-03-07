package com.opspilot.aiorchestrator.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.opspilot.aiorchestrator.domain.entity.Role;
import com.opspilot.aiorchestrator.dto.ChatAskResponse;
import com.opspilot.aiorchestrator.exception.BadRequestException;
import com.opspilot.aiorchestrator.repository.DocumentChunkSearchRepository;
import com.opspilot.aiorchestrator.repository.RetrievedChunk;
import com.opspilot.aiorchestrator.security.CurrentUser;
import com.opspilot.aiorchestrator.service.answering.AnswerGenerationResult;
import com.opspilot.aiorchestrator.service.answering.AnswerService;
import com.opspilot.aiorchestrator.service.embedding.EmbeddingProvider;
import com.opspilot.aiorchestrator.service.embedding.EmbeddingService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private DocumentChunkSearchRepository repository;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private EmbeddingProvider embeddingProvider;

    @Mock
    private AnswerService answerService;

    private ChatService chatService;
    private CurrentUser user;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(repository, embeddingService, answerService, 4, 0.55);
        user = new CurrentUser(UUID.randomUUID(), UUID.randomUUID(), "user@example.com", Role.TENANT_MEMBER);

        lenient().when(embeddingService.provider()).thenReturn(embeddingProvider);
        lenient().when(embeddingProvider.embed(any())).thenReturn(List.of(List.of(0.1, 0.2, 0.3)));
    }

    @Test
    void askShouldReturnCitationsAndComputedConfidence() {
        List<RetrievedChunk> chunks = List.of(
                new RetrievedChunk(UUID.randomUUID(), "policy.txt", 2, "Check in at 15:00", 0.20),
                new RetrievedChunk(UUID.randomUUID(), "policy.txt", 5, "Breakfast starts at 07:00", 0.40)
        );
        when(repository.searchTopChunks(eq(user.tenantId()), any(), eq(2))).thenReturn(chunks);
        when(answerService.generate(any(), eq(chunks))).thenReturn(new AnswerGenerationResult("Answer", "local"));

        ChatAskResponse response = chatService.ask(user, "What are check-in rules?", 2);

        assertEquals("Answer", response.answer());
        assertEquals(0.85, response.confidence());
        assertEquals(2, response.sources().size());
        assertEquals("chunk-2", response.sources().get(0).chunkId());
        assertFalse(response.ticketCreated());
    }

    @Test
    void askShouldReturnZeroConfidenceWhenNoChunks() {
        when(repository.searchTopChunks(eq(user.tenantId()), any(), eq(4))).thenReturn(List.of());
        when(answerService.generate(any(), eq(List.of()))).thenReturn(new AnswerGenerationResult("No context", "local"));

        ChatAskResponse response = chatService.ask(user, "Unknown", null);

        assertEquals(0.0, response.confidence());
        assertEquals(0, response.sources().size());
        assertFalse(response.ticketCreated());
    }

    @Test
    void askShouldRejectOutOfRangeTopK() {
        assertThrows(BadRequestException.class, () -> chatService.ask(user, "Question", 0));
        assertThrows(BadRequestException.class, () -> chatService.ask(user, "Question", 11));
    }
}
