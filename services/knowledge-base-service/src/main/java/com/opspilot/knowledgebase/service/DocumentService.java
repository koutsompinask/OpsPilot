package com.opspilot.knowledgebase.service;

import com.opspilot.knowledgebase.dto.DocumentResponse;
import com.opspilot.knowledgebase.domain.entity.Document;
import com.opspilot.knowledgebase.exception.BadRequestException;
import com.opspilot.knowledgebase.exception.ForbiddenException;
import com.opspilot.knowledgebase.exception.NotFoundException;
import com.opspilot.knowledgebase.util.logging.RequestCorrelation;
import com.opspilot.knowledgebase.repository.DocumentChunkRepository;
import com.opspilot.knowledgebase.repository.DocumentRepository;
import com.opspilot.knowledgebase.security.CurrentUser;
import com.opspilot.knowledgebase.service.storage.DocumentStorageService;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentStorageService documentStorageService;
    private final DocumentIngestionProcessor documentIngestionProcessor;

    public DocumentService(
            DocumentRepository documentRepository,
            DocumentChunkRepository documentChunkRepository,
            DocumentStorageService documentStorageService,
            DocumentIngestionProcessor documentIngestionProcessor
    ) {
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.documentStorageService = documentStorageService;
        this.documentIngestionProcessor = documentIngestionProcessor;
    }

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
                normalizedRequestId
        );
        documentRepository.save(document);

        log.info(
                "knowledge_document_upload_accepted documentId={} tenantId={} userId={} fileName={} requestId={}",
                document.getId(),
                currentUser.tenantId(),
                currentUser.userId(),
                document.getOriginalFilename(),
                normalizedRequestId
        );

        documentIngestionProcessor.processAsync(documentId, normalizedRequestId);
        return DocumentResponse.fromEntity(document);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> list(CurrentUser currentUser) {
        return documentRepository.findByTenantIdOrderByCreatedAtDesc(currentUser.tenantId())
                .stream()
                .map(DocumentResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentResponse get(CurrentUser currentUser, UUID documentId) {
        Document document = documentRepository.findByIdAndTenantId(documentId, currentUser.tenantId())
                .orElseThrow(() -> new NotFoundException("Document not found"));
        return DocumentResponse.fromEntity(document);
    }

    @Transactional
    public void delete(CurrentUser currentUser, UUID documentId) {
        requireAdmin(currentUser);
        Document document = documentRepository.findByIdAndTenantId(documentId, currentUser.tenantId())
                .orElseThrow(() -> new NotFoundException("Document not found"));

        documentChunkRepository.deleteForDocument(document.getId(), document.getTenantId());
        documentStorageService.delete(document.getStorageKey());
        documentRepository.delete(document);

        log.info(
                "knowledge_document_deleted documentId={} tenantId={} userId={} requestId={}",
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
