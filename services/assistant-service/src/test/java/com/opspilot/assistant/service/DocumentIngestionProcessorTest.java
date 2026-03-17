package com.opspilot.assistant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.opspilot.assistant.domain.entity.Document;
import com.opspilot.assistant.repository.DocumentChunkRepository;
import com.opspilot.assistant.repository.DocumentRepository;
import com.opspilot.assistant.service.chunking.TextChunker;
import com.opspilot.assistant.service.embedding.EmbeddingProvider;
import com.opspilot.assistant.service.embedding.EmbeddingService;
import com.opspilot.assistant.service.messaging.DocumentProcessedEventPublisher;
import com.opspilot.assistant.service.storage.DocumentStorageService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentIngestionProcessorTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentChunkRepository documentChunkRepository;

    @Mock
    private DocumentStorageService documentStorageService;

    @Mock
    private TextChunker textChunker;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private EmbeddingProvider embeddingProvider;

    @Mock
    private DocumentProcessedEventPublisher eventPublisher;

    @InjectMocks
    private DocumentIngestionProcessor documentIngestionProcessor;

    @Test
    void processAsyncShouldMarkDocumentFailedWhenStorageReadFails() {
        UUID documentId = UUID.randomUUID();
        Document document = Document.processing(
                documentId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "policy.txt",
                "text/plain",
                "tenant/document/policy.txt",
                "stub:deterministic:1536",
                "req-123"
        );
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentStorageService.loadText(document.getStorageKey())).thenThrow(new RuntimeException("storage offline"));

        documentIngestionProcessor.processAsync(documentId, "req-123");

        assertThat(document.getStatus()).isEqualTo(com.opspilot.assistant.domain.entity.DocumentStatus.FAILED);
        assertThat(document.getErrorMessage()).isEqualTo("storage offline");
        verify(documentRepository).save(document);
        verify(documentChunkRepository, never()).replaceForDocument(any(), any(), any(), any());
        verify(eventPublisher, never()).publish(any(), anyInt(), anyString());
    }
}
