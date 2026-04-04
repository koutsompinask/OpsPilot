package com.opspilot.assistant.service;

import com.opspilot.assistant.dto.DocumentResponse;
import com.opspilot.assistant.domain.entity.Document;
import com.opspilot.assistant.exception.BadRequestException;
import com.opspilot.assistant.exception.ForbiddenException;
import com.opspilot.assistant.exception.NotFoundException;
import com.opspilot.assistant.util.logging.RequestCorrelation;
import com.opspilot.assistant.repository.DocumentChunkRepository;
import com.opspilot.assistant.repository.DocumentRepository;
import com.opspilot.assistant.security.CurrentUser;
import com.opspilot.assistant.service.embedding.EmbeddingService;
import com.opspilot.assistant.service.storage.DocumentStorageService;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Application service for managing tenant documents throughout their lifecycle.
 *
 * <p>Handles document upload validation, object-storage persistence, and async ingestion
 * kick-off, as well as list, get, and delete operations. All operations are tenant-scoped
 * using the {@link CurrentUser#tenantId()} from the authenticated caller. Mutation
 * operations are restricted to {@code TENANT_ADMIN} users; the check is enforced here
 * rather than in the controller so the restriction cannot be accidentally bypassed.</p>
 */
@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentStorageService documentStorageService;
    private final DocumentIngestionProcessor documentIngestionProcessor;
    private final EmbeddingService embeddingService;

    public DocumentService(
            DocumentRepository documentRepository,
            DocumentChunkRepository documentChunkRepository,
            DocumentStorageService documentStorageService,
            DocumentIngestionProcessor documentIngestionProcessor,
            EmbeddingService embeddingService
    ) {
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.documentStorageService = documentStorageService;
        this.documentIngestionProcessor = documentIngestionProcessor;
        this.embeddingService = embeddingService;
    }

    /**
     * Validates, stores, and enqueues a document for async ingestion.
     *
     * <p>The document record is persisted immediately in {@code PROCESSING} status so that
     * callers can poll its state. Actual chunking and embedding happen asynchronously via
     * {@link DocumentIngestionProcessor#processAsync}. The active embedding profile is
     * captured at upload time so that re-indexing can be detected later if the profile changes.</p>
     *
     * @param currentUser the admin user performing the upload
     * @param file        the multipart file to store and ingest
     * @param requestId   the optional correlation ID from the HTTP header; a new UUID is generated if absent
     * @return the persisted document metadata (status will be {@code PROCESSING})
     * @throws com.opspilot.assistant.exception.ForbiddenException    if the caller is not a tenant admin
     * @throws com.opspilot.assistant.exception.BadRequestException   if the file is missing, empty, or not a supported type
     */
    public DocumentResponse create(CurrentUser currentUser, MultipartFile file, String requestId) {
        requireAdmin(currentUser);
        validateFile(file);

        UUID documentId = UUID.randomUUID();
        String normalizedRequestId = RequestCorrelation.normalizeOrGenerate(requestId);
        String storageKey = documentStorageService.store(currentUser.tenantId(), documentId, file);

        Document document = Document.processing(
                documentId,
                currentUser.tenantId(),
                currentUser.userId(),
                file.getOriginalFilename(),
                file.getContentType() == null ? "application/octet-stream" : file.getContentType(),
                storageKey,
                embeddingService.profile().id(),
                normalizedRequestId
        );
        documentRepository.save(document);

        log.info(
                "assistant_document_upload_accepted documentId={} tenantId={} userId={} fileName={} requestId={}",
                document.getId(),
                currentUser.tenantId(),
                currentUser.userId(),
                document.getOriginalFilename(),
                normalizedRequestId
        );

        documentIngestionProcessor.processAsync(documentId, normalizedRequestId);
        return DocumentResponse.fromEntity(document);
    }

    /**
     * Returns a page of documents for the caller's tenant.
     *
     * @param currentUser the authenticated caller used for tenant scoping
     * @param pageable    pagination and sort parameters supplied by the caller
     * @return a page of document metadata records
     */
    @Transactional(readOnly = true)
    public Page<DocumentResponse> list(CurrentUser currentUser, Pageable pageable) {
        return documentRepository.findByTenantId(currentUser.tenantId(), pageable)
                .map(DocumentResponse::fromEntity);
    }

    /**
     * Returns the metadata for a single document, scoped to the caller's tenant.
     *
     * @param currentUser the authenticated caller used for tenant scoping
     * @param documentId  the UUID of the document to retrieve
     * @return the document metadata
     * @throws com.opspilot.assistant.exception.NotFoundException if no document with that ID exists for the tenant
     */
    @Transactional(readOnly = true)
    public DocumentResponse get(CurrentUser currentUser, UUID documentId) {
        Document document = documentRepository.findByIdAndTenantId(documentId, currentUser.tenantId())
                .orElseThrow(() -> new NotFoundException("Document not found"));
        return DocumentResponse.fromEntity(document);
    }

    /**
     * Deletes a document, all its associated chunks, and the underlying storage object.
     *
     * <p>Deletion is performed in dependency order: chunks first, then storage, then the
     * document record. This ensures that a partial failure (e.g. storage service unavailable)
     * does not leave orphaned chunk rows referencing a deleted document.</p>
     *
     * @param currentUser the admin user performing the delete
     * @param documentId  the UUID of the document to delete
     * @throws com.opspilot.assistant.exception.ForbiddenException  if the caller is not a tenant admin
     * @throws com.opspilot.assistant.exception.NotFoundException   if no document with that ID exists for the tenant
     */
    @Transactional
    public void delete(CurrentUser currentUser, UUID documentId) {
        requireAdmin(currentUser);
        Document document = documentRepository.findByIdAndTenantId(documentId, currentUser.tenantId())
                .orElseThrow(() -> new NotFoundException("Document not found"));

        documentChunkRepository.deleteForDocument(document.getId(), document.getTenantId());
        documentStorageService.delete(document.getStorageKey());
        documentRepository.delete(document);

        log.info(
                "assistant_document_deleted documentId={} tenantId={} userId={} requestId={}",
                document.getId(),
                currentUser.tenantId(),
                currentUser.userId(),
                RequestCorrelation.currentRequestId()
        );
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Document file is required");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new BadRequestException("Document filename is required");
        }

        String lower = originalName.toLowerCase(Locale.ROOT);
        if (!(lower.endsWith(".txt") || lower.endsWith(".md"))) {
            throw new BadRequestException("Only .txt and .md files are supported in Phase 3");
        }
    }

    private void requireAdmin(CurrentUser currentUser) {
        if (!currentUser.isAdmin()) {
            throw new ForbiddenException("Only tenant admins can modify documents");
        }
    }
}
