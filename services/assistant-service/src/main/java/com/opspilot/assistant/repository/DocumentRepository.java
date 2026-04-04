package com.opspilot.assistant.repository;

import com.opspilot.assistant.domain.entity.Document;
import com.opspilot.assistant.domain.entity.DocumentStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA repository for {@link Document} entities, scoped to the {@code assistant.documents} table.
 *
 * All query methods enforce tenant isolation by requiring {@code tenantId} as a parameter.
 */
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    /** Returns all documents for a tenant, ordered newest-first. */
    List<Document> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

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
}
