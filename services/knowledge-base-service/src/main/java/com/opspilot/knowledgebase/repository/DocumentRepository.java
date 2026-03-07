package com.opspilot.knowledgebase.repository;

import com.opspilot.knowledgebase.domain.entity.Document;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    Optional<Document> findByIdAndTenantId(UUID id, UUID tenantId);
}
