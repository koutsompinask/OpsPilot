package com.opspilot.assistant.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.opspilot.assistant.domain.entity.Role;
import com.opspilot.assistant.dto.ChatAskResponse;
import com.opspilot.assistant.exception.BadRequestException;
import com.opspilot.assistant.repository.RetrievedChunk;
import com.opspilot.assistant.security.CurrentUser;
import com.opspilot.assistant.service.answering.AnswerGenerationResult;
import com.opspilot.assistant.service.answering.AnswerService;
import com.opspilot.assistant.service.embedding.EmbeddingProfile;
import com.opspilot.assistant.service.embedding.EmbeddingService;
import com.opspilot.assistant.service.integration.TicketClient;
import com.opspilot.assistant.service.retrieval.ChunkRetrievalService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChunkRetrievalService chunkRetrievalService;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private DocumentEmbeddingMaintenanceService documentEmbeddingMaintenanceService;

    @Mock
    private AnswerService answerService;

    @Mock
    private TicketClient ticketClient;

    private ChatService chatService;
    private CurrentUser user;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(chunkRetrievalService, documentEmbeddingMaintenanceService, embeddingService, answerService, ticketClient, 4, 0.55);
        user = new CurrentUser(UUID.randomUUID(), UUID.randomUUID(), "user@example.com", Role.TENANT_MEMBER);

        lenient().when(documentEmbeddingMaintenanceService.ensureCurrentProfile(any(), any()))
                .thenReturn(new EmbeddingIndexRefreshResult(0, 1));
        lenient().when(embeddingService.profile()).thenReturn(new EmbeddingProfile("tei:test:384", "tei", "test", 384));
    }

    @Test
    void askShouldReturnEvidenceAndComputedConfidence() {
        List<RetrievedChunk> chunks = List.of(
                new RetrievedChunk(UUID.randomUUID(), "policy.txt", 2, "Front Desk Operations", "paragraph",
                        "Check-in time starts at 15:00 local time. Check-out time is 11:00 local time.",
                        0.12, 0.87, 0.032, 0.92),
                new RetrievedChunk(UUID.randomUUID(), "policy.txt", 5, "Breakfast", "paragraph",
                        "Breakfast starts at 07:00.", 0.35, 0.44, 0.016, 0.41)
        );
        when(chunkRetrievalService.retrieve(eq(user.tenantId()), eq("What are check-in rules?"), eq(2))).thenReturn(chunks);
        when(answerService.generate(any(), eq(chunks))).thenReturn(new AnswerGenerationResult(
                "Check-in starts at 15:00 and check-out is 11:00 local time.",
                "Matched Front Desk Operations in policy.txt.",
                "extractive",
                "extractive-grounded"
        ));

        ChatAskResponse response = chatService.ask(user, "What are check-in rules?", 2);

        assertEquals("Check-in starts at 15:00 and check-out is 11:00 local time.", response.answer());
        assertEquals("Matched Front Desk Operations in policy.txt.", response.reasoningSummary());
        assertEquals(0.665, response.confidence());
        assertEquals(2, response.sources().size());
        assertEquals(2, response.evidence().size());
        assertEquals("Front Desk Operations", response.evidence().getFirst().sectionTitle());
        assertEquals("extractive-grounded", response.answerMode());
        assertFalse(response.ticketCreated());
    }

    @Test
    void askShouldReturnZeroConfidenceWhenNoChunks() {
        when(chunkRetrievalService.retrieve(eq(user.tenantId()), eq("Unknown"), eq(4))).thenReturn(List.of());
        when(answerService.generate(any(), eq(List.of()))).thenReturn(new AnswerGenerationResult(
                "No context",
                "No evidence",
                "extractive",
                "insufficient-evidence"
        ));

        ChatAskResponse response = chatService.ask(user, "Unknown", null);

        assertEquals(0.0, response.confidence());
        assertEquals(0, response.sources().size());
        org.junit.jupiter.api.Assertions.assertTrue(response.ticketCreated());
    }

    @Test
    void askShouldKeepResponseWhenTicketCreateFails() {
        when(chunkRetrievalService.retrieve(eq(user.tenantId()), eq("Unknown"), eq(4))).thenReturn(List.of());
        when(answerService.generate(any(), eq(List.of()))).thenReturn(new AnswerGenerationResult(
                "No context",
                "No evidence",
                "extractive",
                "insufficient-evidence"
        ));
        org.mockito.Mockito.doThrow(new IllegalStateException("boom")).when(ticketClient).createTicket(any());

        ChatAskResponse response = chatService.ask(user, "Unknown", null);

        assertEquals("No context", response.answer());
        assertFalse(response.ticketCreated());
    }

    @Test
    void askShouldRejectOutOfRangeTopK() {
        assertThrows(BadRequestException.class, () -> chatService.ask(user, "Question", 0));
        assertThrows(BadRequestException.class, () -> chatService.ask(user, "Question", 11));
    }

    @Test
    void askShouldReturnReindexMessageWhenNoActiveProfileDocumentsAreReady() {
        when(documentEmbeddingMaintenanceService.ensureCurrentProfile(any(), any()))
                .thenReturn(new EmbeddingIndexRefreshResult(2, 0));
        when(chunkRetrievalService.retrieve(eq(user.tenantId()), eq("Unknown"), eq(4))).thenReturn(List.of());

        ChatAskResponse response = chatService.ask(user, "Unknown", null);

        assertEquals(
                "Your knowledge base is being re-indexed for the active embedding model. Please retry in a moment.",
                response.answer()
        );
        assertEquals(0.0, response.confidence());
        assertFalse(response.ticketCreated());
    }
}
