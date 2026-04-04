package com.opspilot.assistant.service;

import com.opspilot.assistant.service.chunking.TextChunker;
import com.opspilot.assistant.service.chunking.TextChunk;
import com.opspilot.assistant.domain.entity.Document;
import com.opspilot.assistant.service.messaging.DocumentProcessedEventPublisher;
import com.opspilot.assistant.repository.DocumentChunkRepository;
import com.opspilot.assistant.repository.DocumentRepository;
import com.opspilot.assistant.service.embedding.EmbeddingProfile;
import com.opspilot.assistant.service.embedding.EmbeddingService;
import com.opspilot.assistant.service.storage.DocumentStorageService;
import com.opspilot.assistant.util.logging.RequestCorrelation;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Asynchronous pipeline that converts an uploaded document into searchable embedding chunks.
 *
 * <p>Processing steps executed on the {@code ingestionExecutor} thread pool:
 * <ol>
 *   <li>Load the raw document text from object storage.</li>
 *   <li>Split the text into structured {@link com.opspilot.assistant.service.chunking.TextChunk}s
 *       using the block-aware {@link TextChunker}.</li>
 *   <li>Embed all chunk texts in a single batch call to the active embedding provider.</li>
 *   <li>Atomically replace any existing chunks for the document with the new embeddings.</li>
 *   <li>Transition the document status to {@code READY} and publish a
 *       {@code document.processed} event over RabbitMQ.</li>
 * </ol>
 * Any exception during steps 1–4 transitions the document to {@code FAILED} status with
 * a truncated error message stored on the entity, so the error is visible via the API.
 * The event-publish step (step 5) is wrapped in its own try-catch so that a messaging
 * failure does not roll back the already-successful ingestion.</p>
 */
@Service
public class DocumentIngestionProcessor {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionProcessor.class);

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentStorageService documentStorageService;
    private final TextChunker textChunker;
    private final EmbeddingService embeddingService;
    private final DocumentProcessedEventPublisher eventPublisher;

    public DocumentIngestionProcessor(
            DocumentRepository documentRepository,
            DocumentChunkRepository documentChunkRepository,
            DocumentStorageService documentStorageService,
            TextChunker textChunker,
            EmbeddingService embeddingService,
            DocumentProcessedEventPublisher eventPublisher
    ) {
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.documentStorageService = documentStorageService;
        this.textChunker = textChunker;
        this.embeddingService = embeddingService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Ingests a document asynchronously on the {@code ingestionExecutor} thread pool.
     *
     * <p>The correlation ID is installed into the MDC at the start so that all log lines
     * emitted during ingestion carry the same request ID as the original upload request,
     * enabling end-to-end trace correlation across the async boundary.</p>
     *
     * @param documentId the UUID of the document to ingest (must already be persisted in {@code PROCESSING} status)
     * @param requestId  the correlation ID propagated from the upload request
     */
    @Async("ingestionExecutor")
    @Transactional
    public void processAsync(UUID documentId, String requestId) {
        // Re-establish the correlation ID in this thread's MDC because the async executor
        // starts a new thread where the original request-scoped MDC is absent.
        MDC.put(RequestCorrelation.MDC_KEY, RequestCorrelation.normalizeOrGenerate(requestId));
        try {
            Document document = documentRepository.findById(documentId).orElse(null);
            if (document == null) {
                log.warn("assistant_document_ingestion_skipped_missing_document documentId={} requestId={}", documentId, requestId);
                return;
            }

            log.info("assistant_document_ingestion_started documentId={} tenantId={} requestId={}", documentId, document.getTenantId(), requestId);

            String content = documentStorageService.loadText(document.getStorageKey());
            List<TextChunk> chunks = textChunker.chunk(content);
            if (chunks.isEmpty()) {
                throw new IllegalArgumentException("Uploaded document has no text content");
            }

            EmbeddingProfile profile = embeddingService.profile();
            // Embed all chunk texts in one batch to minimise round-trips to the embedding provider
            List<List<Double>> embeddings = embeddingService.provider().embed(chunks.stream().map(TextChunk::text).toList());
            // Atomically delete old chunks and insert new ones so the document is never partially indexed
            documentChunkRepository.replaceForDocument(document.getId(), document.getTenantId(), chunks, embeddings);
            document.markReady(chunks.size(), profile.id());
            documentRepository.save(document);

            try {
                // Best-effort event publish — a messaging failure must not roll back the ingestion transaction
                eventPublisher.publish(document, chunks.size(), document.getCreatedRequestId());
            } catch (Exception ex) {
                log.error(
                        "assistant_document_processed_event_publish_failed documentId={} tenantId={} requestId={} reason={}",
                        document.getId(),
                        document.getTenantId(),
                        document.getCreatedRequestId(),
                        ex.getMessage(),
                        ex
                );
            }

            log.info(
                    "assistant_document_ingestion_completed documentId={} tenantId={} chunkCount={} requestId={}",
                    document.getId(),
                    document.getTenantId(),
                    chunks.size(),
                    document.getCreatedRequestId()
            );
        } catch (Exception ex) {
            documentRepository.findById(documentId).ifPresent(document -> {
                document.markFailed(trim(ex.getMessage()));
                documentRepository.save(document);
                log.error(
                        "assistant_document_ingestion_failed documentId={} tenantId={} requestId={} reason={}",
                        document.getId(),
                        document.getTenantId(),
                        document.getCreatedRequestId(),
                        ex.getMessage(),
                        ex
                );
            });
        } finally {
            MDC.remove(RequestCorrelation.MDC_KEY);
        }
    }

    private String trim(String message) {
        if (message == null || message.isBlank()) {
            return "Document ingestion failed";
        }
        return message.length() > 250 ? message.substring(0, 250) : message;
    }
}
