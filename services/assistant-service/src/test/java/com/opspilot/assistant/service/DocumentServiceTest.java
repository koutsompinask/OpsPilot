package com.opspilot.assistant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.opspilot.assistant.domain.entity.Document;
import com.opspilot.assistant.domain.entity.DocumentStatus;
import com.opspilot.assistant.domain.entity.Role;
import com.opspilot.assistant.exception.BadRequestException;
import com.opspilot.assistant.exception.ForbiddenException;
import com.opspilot.assistant.repository.DocumentChunkRepository;
import com.opspilot.assistant.repository.DocumentRepository;
import com.opspilot.assistant.security.CurrentUser;
import com.opspilot.assistant.service.embedding.EmbeddingProfile;
import com.opspilot.assistant.service.embedding.EmbeddingService;
import com.opspilot.assistant.service.storage.DocumentStorageService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentChunkRepository documentChunkRepository;

    @Mock
    private DocumentStorageService documentStorageService;

    @Mock
    private DocumentIngestionProcessor documentIngestionProcessor;

    @Mock
    private EmbeddingService embeddingService;

    @InjectMocks
    private DocumentService documentService;

    private CurrentUser adminUser;
    private CurrentUser memberUser;

    @BeforeEach
    void setUp() {
        UUID tenantId = UUID.randomUUID();
        adminUser = new CurrentUser(UUID.randomUUID(), tenantId, "admin@example.com", Role.TENANT_ADMIN);
        memberUser = new CurrentUser(UUID.randomUUID(), tenantId, "member@example.com", Role.TENANT_MEMBER);
        lenient().when(embeddingService.profile()).thenReturn(new EmbeddingProfile("tei:test:384", "tei", "test", 384));
    }

    @Test
    void createShouldRejectNonAdminUser() {
        MultipartFile file = new MockMultipartFile("file", "policy.txt", "text/plain", "policy".getBytes());

        assertThatThrownBy(() -> documentService.create(memberUser, file, "req-1"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Only tenant admins can modify documents");

        verifyNoInteractions(documentStorageService, documentRepository, documentIngestionProcessor);
    }

    @Test
    void createShouldRejectUnsupportedExtension() {
        MultipartFile file = new MockMultipartFile("file", "policy.pdf", "application/pdf", "pdf".getBytes());

        assertThatThrownBy(() -> documentService.create(adminUser, file, "req-1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Only .txt and .md files are supported in Phase 3");

        verifyNoInteractions(documentStorageService, documentRepository, documentIngestionProcessor);
    }

    @Test
    void createShouldPersistDocumentAndTriggerAsyncIngestionWithNormalizedRequestId() {
        MultipartFile file = new MockMultipartFile("file", "policy.txt", "text/plain", "policy".getBytes());
        when(documentStorageService.store(any(), any(), any())).thenReturn("tenant/document/policy.txt");

        var response = documentService.create(adminUser, file, "  req-123  ");

        ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(documentCaptor.capture());
        verify(documentIngestionProcessor).processAsync(response.id(), "req-123");

        Document saved = documentCaptor.getValue();
        assertThat(response.id()).isEqualTo(saved.getId());
        assertThat(saved.getTenantId()).isEqualTo(adminUser.tenantId());
        assertThat(saved.getUploadedBy()).isEqualTo(adminUser.userId());
        assertThat(saved.getOriginalFilename()).isEqualTo("policy.txt");
        assertThat(saved.getContentType()).isEqualTo("text/plain");
        assertThat(saved.getStorageKey()).isEqualTo("tenant/document/policy.txt");
        assertThat(saved.getCreatedRequestId()).isEqualTo("req-123");
        assertThat(saved.getStatus()).isEqualTo(DocumentStatus.PROCESSING);
    }

    @Test
    void deleteShouldRemoveChunksStorageAndMetadata() {
        UUID documentId = UUID.randomUUID();
        Document document = Document.processing(
                documentId,
                adminUser.tenantId(),
                adminUser.userId(),
                "policy.txt",
                "text/plain",
                "tenant/document/policy.txt",
                "stub:deterministic:1536",
                "req-123"
        );
        when(documentRepository.findByIdAndTenantId(documentId, adminUser.tenantId())).thenReturn(Optional.of(document));

        documentService.delete(adminUser, documentId);

        verify(documentChunkRepository).deleteForDocument(documentId, adminUser.tenantId());
        verify(documentStorageService).delete("tenant/document/policy.txt");
        verify(documentRepository).delete(document);
    }
}
