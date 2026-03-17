package com.opspilot.assistant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.opspilot.assistant.domain.entity.Document;
import com.opspilot.assistant.domain.entity.DocumentStatus;
import com.opspilot.assistant.repository.DocumentRepository;
import com.opspilot.assistant.service.embedding.EmbeddingProfile;
import com.opspilot.assistant.service.embedding.EmbeddingService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentEmbeddingMaintenanceServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentIngestionProcessor documentIngestionProcessor;

    @Mock
    private EmbeddingService embeddingService;

    @InjectMocks
    private DocumentEmbeddingMaintenanceService service;

    @BeforeEach
    void setUp() {
        lenient().when(embeddingService.profile()).thenReturn(new EmbeddingProfile("tei:test:384", "tei", "test", 384));
    }

    @Test
    void ensureCurrentProfileShouldScheduleOutdatedDocumentsAndReturnReadyCount() {
        UUID tenantId = UUID.randomUUID();
        Document outdatedReady = Document.processing(
                UUID.randomUUID(),
                tenantId,
                UUID.randomUUID(),
                "policy.txt",
                "text/plain",
                "tenant/policy.txt",
                "legacy-local-1536",
                "req-old"
        );
        outdatedReady.markReady(2, "legacy-local-1536");
        Document alreadyProcessing = Document.processing(
                UUID.randomUUID(),
                tenantId,
                UUID.randomUUID(),
                "faq.txt",
                "text/plain",
                "tenant/faq.txt",
                "legacy-local-1536",
                "req-old"
        );

        when(documentRepository.findByTenantIdAndEmbeddingProfileNotOrderByCreatedAtAsc(tenantId, "tei:test:384"))
                .thenReturn(List.of(outdatedReady, alreadyProcessing));
        when(documentRepository.countByTenantIdAndStatusAndEmbeddingProfile(tenantId, DocumentStatus.READY, "tei:test:384"))
                .thenReturn(3L);

        EmbeddingIndexRefreshResult result = service.ensureCurrentProfile(tenantId, " req-123 ");

        assertThat(result.scheduledCount()).isEqualTo(1);
        assertThat(result.readyDocumentCount()).isEqualTo(3L);
        assertThat(outdatedReady.getStatus()).isEqualTo(DocumentStatus.PROCESSING);
        verify(documentRepository).save(outdatedReady);
        verify(documentIngestionProcessor).processAsync(outdatedReady.getId(), "req-123");
    }

    @Test
    void ensureCurrentProfileShouldSkipWhenEverythingIsCurrentOrAlreadyProcessing() {
        UUID tenantId = UUID.randomUUID();
        when(documentRepository.findByTenantIdAndEmbeddingProfileNotOrderByCreatedAtAsc(tenantId, "tei:test:384"))
                .thenReturn(List.of());
        when(documentRepository.countByTenantIdAndStatusAndEmbeddingProfile(tenantId, DocumentStatus.READY, "tei:test:384"))
                .thenReturn(1L);

        EmbeddingIndexRefreshResult result = service.ensureCurrentProfile(tenantId, "req-1");

        assertThat(result.scheduledCount()).isZero();
        assertThat(result.readyDocumentCount()).isEqualTo(1L);
        verifyNoInteractions(documentIngestionProcessor);
        verify(documentRepository).countByTenantIdAndStatusAndEmbeddingProfile(tenantId, DocumentStatus.READY, "tei:test:384");
    }
}
