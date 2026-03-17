package com.opspilot.assistant.repository;

import com.opspilot.assistant.domain.entity.Document;
import com.opspilot.assistant.domain.entity.DocumentStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<Document> findByTenantIdAndEmbeddingProfileNotOrderByCreatedAtAsc(UUID tenantId, String embeddingProfile);

    long countByTenantIdAndStatusAndEmbeddingProfile(UUID tenantId, DocumentStatus status, String embeddingProfile);

    Optional<Document> findByIdAndTenantId(UUID id, UUID tenantId);
}
