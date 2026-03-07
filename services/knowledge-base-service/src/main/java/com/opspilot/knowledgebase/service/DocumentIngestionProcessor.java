package com.opspilot.knowledgebase.service;

import com.opspilot.knowledgebase.service.chunking.TextChunker;
import com.opspilot.knowledgebase.service.embedding.EmbeddingService;
import com.opspilot.knowledgebase.domain.entity.Document;
import com.opspilot.knowledgebase.util.logging.RequestCorrelation;
import com.opspilot.knowledgebase.service.messaging.DocumentProcessedEventPublisher;
import com.opspilot.knowledgebase.repository.DocumentChunkRepository;
import com.opspilot.knowledgebase.repository.DocumentRepository;
import com.opspilot.knowledgebase.service.storage.DocumentStorageService;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Async("ingestionExecutor")
    @Transactional
    public void processAsync(UUID documentId, String requestId) {
        MDC.put(RequestCorrelation.MDC_KEY, RequestCorrelation.normalizeOrGenerate(requestId));
        try {
            Document document = documentRepository.findById(documentId).orElse(null);
            if (document == null) {
                log.warn("knowledge_document_ingestion_skipped_missing_document documentId={} requestId={}", documentId, requestId);
                return;
            }

            log.info("knowledge_document_ingestion_started documentId={} tenantId={} requestId={}", documentId, document.getTenantId(), requestId);

            String content = documentStorageService.loadText(document.getStorageKey());
            List<String> chunks = textChunker.chunk(content);
            if (chunks.isEmpty()) {
                throw new IllegalArgumentException("Uploaded document has no text content");
            }

            List<List<Double>> embeddings = embeddingService.provider().embed(chunks);
            documentChunkRepository.replaceForDocument(document.getId(), document.getTenantId(), chunks, embeddings);
            document.markReady(chunks.size());
            documentRepository.save(document);

            try {
                eventPublisher.publish(document, chunks.size(), document.getCreatedRequestId());
            } catch (Exception ex) {
                log.error(
                        "knowledge_document_processed_event_publish_failed documentId={} tenantId={} requestId={} reason={}",
                        document.getId(),
                        document.getTenantId(),
                        document.getCreatedRequestId(),
                        ex.getMessage(),
                        ex
                );
            }

            log.info(
                    "knowledge_document_ingestion_completed documentId={} tenantId={} chunkCount={} requestId={}",
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
                        "knowledge_document_ingestion_failed documentId={} tenantId={} requestId={} reason={}",
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
