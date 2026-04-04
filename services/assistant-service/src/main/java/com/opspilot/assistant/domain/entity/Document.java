package com.opspilot.assistant.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing a tenant-scoped knowledge document.
 *
 * A document moves through the lifecycle: {@code PROCESSING} (upload accepted, ingestion running)
 * → {@code READY} (chunks embedded and indexed) or {@code FAILED} (ingestion error).
 * State transitions are encapsulated in {@link #markReady}, {@link #markFailed}, and
 * {@link #markProcessing}.
 *
 * The {@code storageKey} field is the MinIO object key where the raw file is stored.
 * The {@code embeddingProfile} identifies which embedding model configuration was used,
 * enabling detection of documents that need re-indexing after a profile change.
 */
@Entity
@Table(name = "documents", schema = "assistant")
public class Document {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Column(name = "embedding_profile", nullable = false)
    private String embeddingProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DocumentStatus status;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "chunk_count", nullable = false)
    private int chunkCount;

    @Column(name = "created_request_id", nullable = false)
    private String createdRequestId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Factory method — creates a new document record in {@code PROCESSING} state immediately
     * after the file is uploaded and before async ingestion begins.
     */
    public static Document processing(
            UUID id,
            UUID tenantId,
            UUID uploadedBy,
            String originalFilename,
            String contentType,
            String storageKey,
            String embeddingProfile,
            String createdRequestId
    ) {
        Document document = new Document();
        document.id = id;
        document.tenantId = tenantId;
        document.uploadedBy = uploadedBy;
        document.originalFilename = originalFilename;
        document.contentType = contentType;
        document.storageKey = storageKey;
        document.embeddingProfile = embeddingProfile;
        document.createdRequestId = createdRequestId;
        document.status = DocumentStatus.PROCESSING;
        document.chunkCount = 0;
        return document;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    /** Transitions the document to {@code READY} after successful ingestion and indexing. */
    public void markReady(int chunkCount, String embeddingProfile) {
        this.status = DocumentStatus.READY;
        this.chunkCount = chunkCount;
        this.embeddingProfile = embeddingProfile;
        this.errorMessage = null;
    }

    /** Transitions the document to {@code FAILED} and records the error message. */
    public void markFailed(String errorMessage) {
        this.status = DocumentStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    /** Resets the document to {@code PROCESSING} state, used when re-triggering ingestion. */
    public void markProcessing(String requestId) {
        this.status = DocumentStatus.PROCESSING;
        this.chunkCount = 0;
        this.errorMessage = null;
        this.createdRequestId = requestId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getUploadedBy() {
        return uploadedBy;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getEmbeddingProfile() {
        return embeddingProfile;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getChunkCount() {
        return chunkCount;
    }

    public String getCreatedRequestId() {
        return createdRequestId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
