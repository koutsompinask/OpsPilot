package com.opspilot.assistant.service;

import com.opspilot.assistant.domain.entity.Document;
import com.opspilot.assistant.domain.entity.DocumentStatus;
import com.opspilot.assistant.repository.DocumentRepository;
import com.opspilot.assistant.service.embedding.EmbeddingService;
import com.opspilot.assistant.util.logging.RequestCorrelation;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentEmbeddingMaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(DocumentEmbeddingMaintenanceService.class);

    private final DocumentRepository documentRepository;
    private final DocumentIngestionProcessor documentIngestionProcessor;
    private final EmbeddingService embeddingService;

    public DocumentEmbeddingMaintenanceService(
            DocumentRepository documentRepository,
            DocumentIngestionProcessor documentIngestionProcessor,
            EmbeddingService embeddingService
    ) {
        this.documentRepository = documentRepository;
        this.documentIngestionProcessor = documentIngestionProcessor;
        this.embeddingService = embeddingService;
    }

    @Transactional
    public EmbeddingIndexRefreshResult ensureCurrentProfile(UUID tenantId, String requestId) {
        String normalizedRequestId = RequestCorrelation.normalizeOrGenerate(requestId);
        String activeProfile = embeddingService.profile().id();

        List<Document> outdated = documentRepository.findByTenantIdAndEmbeddingProfileNotOrderByCreatedAtAsc(tenantId, activeProfile)
                .stream()
                .filter(document -> document.getStatus() != DocumentStatus.PROCESSING)
                .toList();

        for (Document document : outdated) {
            document.markProcessing(normalizedRequestId);
            documentRepository.save(document);
            documentIngestionProcessor.processAsync(document.getId(), normalizedRequestId);
        }

        if (!outdated.isEmpty()) {
            log.info(
                    "assistant_embedding_reindex_scheduled tenantId={} scheduledCount={} profile={} requestId={}",
                    tenantId,
                    outdated.size(),
                    activeProfile,
                    normalizedRequestId
            );
        }

        long readyCount = documentRepository.countByTenantIdAndStatusAndEmbeddingProfile(
                tenantId,
                DocumentStatus.READY,
                activeProfile
        );
        return new EmbeddingIndexRefreshResult(outdated.size(), readyCount);
    }
}
