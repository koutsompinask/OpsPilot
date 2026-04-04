package com.opspilot.assistant.repository;

import com.opspilot.assistant.domain.entity.Document;
import com.opspilot.assistant.domain.entity.DocumentStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA repository for {@link Document} entities, scoped to the {@code assistant.documents} table.
 *
 * All query methods enforce tenant isolation by requiring {@code tenantId} as a parameter.
 */
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    /** Returns all documents for a tenant, ordered newest-first. */
    List<Document> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    /** Returns a page of documents for a tenant; sort and page size are controlled by the caller via {@code pageable}. */
    Page<Document> findByTenantId(UUID tenantId, Pageable pageable);

    /**
     * Returns documents whose {@code embeddingProfile} does not match the given profile,
     * used to identify documents that need re-indexing after a profile switch.
     */
    List<Document> findByTenantIdAndEmbeddingProfileNotOrderByCreatedAtAsc(UUID tenantId, String embeddingProfile);

    /** Counts documents with a specific status and embedding profile for a tenant. */
    long countByTenantIdAndStatusAndEmbeddingProfile(UUID tenantId, DocumentStatus status, String embeddingProfile);

    /**
     * Finds a document by ID and tenant ID, returning empty if the ID does not exist or
     * belongs to a different tenant.
     */
    Optional<Document> findByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * Returns all documents in {@code PROCESSING} state whose {@code updatedAt} timestamp
     * is older than the given cutoff. Used by the stuck-document watchdog to identify
     * ingestion tasks that never completed (e.g. due to a service restart or uncaught error).
     *
     * @param status  must be {@link DocumentStatus#PROCESSING}
     * @param cutoff  documents updated before this instant are considered stuck
     * @return list of stuck documents across all tenants
     */
    List<Document> findByStatusAndUpdatedAtBefore(DocumentStatus status, Instant cutoff);
}
