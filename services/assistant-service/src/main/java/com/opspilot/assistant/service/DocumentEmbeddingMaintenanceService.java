package com.opspilot.assistant.service;

import com.opspilot.assistant.domain.entity.Document;
import com.opspilot.assistant.domain.entity.DocumentStatus;
import com.opspilot.assistant.repository.DocumentRepository;
import com.opspilot.assistant.service.embedding.EmbeddingService;
import com.opspilot.assistant.util.logging.RequestCorrelation;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Detects and repairs embedding-profile drift for a tenant's document index.
 *
 * <p>When the active embedding profile changes (e.g. the operator switches from a local
 * deterministic model to a production-grade TEI model), existing document chunks are
 * still stored under the old profile's vectors and will not be retrieved by queries
 * encoded with the new profile. This service detects such stale documents and
 * re-triggers the ingestion pipeline for each of them so they are re-chunked and
 * re-embedded under the new profile. The check is performed on every chat request,
 * before retrieval, so the first query after a profile switch transparently initiates
 * the background migration.</p>
 */
@Service
public class DocumentEmbeddingMaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(DocumentEmbeddingMaintenanceService.class);

    // Documents in PROCESSING beyond this threshold are assumed to be stuck (e.g. the
    // service restarted mid-ingestion) and are marked FAILED by the watchdog
    private final Duration stuckProcessingTimeout;

    private final DocumentRepository documentRepository;
    private final DocumentIngestionProcessor documentIngestionProcessor;
    private final EmbeddingService embeddingService;

    public DocumentEmbeddingMaintenanceService(
            DocumentRepository documentRepository,
            DocumentIngestionProcessor documentIngestionProcessor,
            EmbeddingService embeddingService,
            @Value("${assistant.ingestion.stuck-timeout-minutes:30}") long stuckTimeoutMinutes
    ) {
        this.documentRepository = documentRepository;
        this.documentIngestionProcessor = documentIngestionProcessor;
        this.embeddingService = embeddingService;
        this.stuckProcessingTimeout = Duration.ofMinutes(stuckTimeoutMinutes);
    }

    /**
     * Ensures that all of the tenant's documents are indexed under the currently active
     * embedding profile, scheduling re-ingestion for any that are not.
     *
     * <p>Documents already in {@code PROCESSING} status are skipped to avoid duplicate
     * concurrent ingestion runs for the same document.</p>
     *
     * @param tenantId  the tenant whose document index should be checked
     * @param requestId the correlation ID from the originating chat request, propagated
     *                  into the async re-ingestion tasks for end-to-end tracing
     * @return a summary containing the number of documents scheduled for re-ingestion
     *         and the count of documents already {@code READY} under the active profile
     */
    @Transactional
    public EmbeddingIndexRefreshResult ensureCurrentProfile(UUID tenantId, String requestId) {
        String normalizedRequestId = RequestCorrelation.normalizeOrGenerate(requestId);
        String activeProfile = embeddingService.profile().id();

        // Find documents whose stored embedding_profile differs from the currently active one.
        // Skip any that are already being processed to avoid double-scheduling.
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

    /**
     * Periodic watchdog that marks any document stuck in {@code PROCESSING} as {@code FAILED}.
     *
     * <p>A document is considered stuck if it has been in {@code PROCESSING} state for longer
     * than {@code assistant.ingestion.stuck-timeout-minutes} (default: 30 minutes). This covers
     * the case where the service restarts mid-ingestion, leaving the async task orphaned with
     * no thread to complete it. Running every 15 minutes provides recovery within one check
     * interval after the timeout expires.</p>
     */
    @Scheduled(fixedDelayString = "${assistant.ingestion.watchdog-interval-ms:900000}")
    @Transactional
    public void markStuckDocumentsAsFailed() {
        Instant cutoff = Instant.now().minus(stuckProcessingTimeout);
        List<Document> stuck = documentRepository.findByStatusAndUpdatedAtBefore(DocumentStatus.PROCESSING, cutoff);
        if (stuck.isEmpty()) {
            return;
        }
        for (Document document : stuck) {
            document.markFailed("Ingestion timed out — document was stuck in PROCESSING for more than " + stuckProcessingTimeout.toMinutes() + " minutes");
            documentRepository.save(document);
        }
        log.warn(
                "assistant_stuck_documents_failed count={} timeoutMinutes={}",
                stuck.size(),
                stuckProcessingTimeout.toMinutes()
        );
    }
}
