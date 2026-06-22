package com.opspilot.assistant.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.opspilot.assistant.domain.entity.Document;
import com.opspilot.assistant.domain.entity.Role;
import com.opspilot.assistant.dto.DocumentResponse;
import com.opspilot.assistant.exception.GlobalExceptionHandler;
import com.opspilot.assistant.security.CurrentUser;
import com.opspilot.assistant.security.CurrentUserResolver;
import com.opspilot.assistant.service.DocumentService;
import com.opspilot.assistant.util.logging.RequestCorrelation;
import java.util.UUID;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = DocumentController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CurrentUserResolver currentUserResolver;

    @MockBean
    private DocumentService documentService;

    @Test
    void uploadShouldPassMultipartAndRequestIdToService() throws Exception {
        CurrentUser user = new CurrentUser(UUID.randomUUID(), UUID.randomUUID(), "admin@example.com", Role.TENANT_ADMIN);
        Document document = Document.processing(
                UUID.randomUUID(),
                user.tenantId(),
                user.userId(),
                "policy.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "tenant/doc/policy.txt",
                "tei:test:384",
                "req-123"
        );
        when(currentUserResolver.fromJwt(any())).thenReturn(user);
        when(documentService.create(eq(user), any(), eq("req-123"))).thenReturn(DocumentResponse.fromEntity(document));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "policy.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Check in starts at 15:00".getBytes()
        );

        mockMvc.perform(multipart("/documents")
                        .file(file)
                        .header(RequestCorrelation.HEADER_NAME, "req-123"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(document.getId().toString()))
                .andExpect(jsonPath("$.filename").value("policy.txt"))
                .andExpect(jsonPath("$.status").value("PROCESSING"));

        verify(documentService).create(eq(user), any(), eq("req-123"));
    }

    @Test
    void listShouldReturnTenantDocuments() throws Exception {
        CurrentUser user = new CurrentUser(UUID.randomUUID(), UUID.randomUUID(), "member@example.com", Role.TENANT_MEMBER);
        Document document = Document.processing(
                UUID.randomUUID(),
                user.tenantId(),
                user.userId(),
                "faq.md",
                "text/markdown",
                "tenant/doc/faq.md",
                "tei:test:384",
                "req-list"
        );
        when(currentUserResolver.fromJwt(any())).thenReturn(user);
        when(documentService.list(eq(user), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of(DocumentResponse.fromEntity(document))));

        mockMvc.perform(get("/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(document.getId().toString()))
                .andExpect(jsonPath("$.content[0].filename").value("faq.md"));
    }

    @Test
    void deleteShouldReturnNoContent() throws Exception {
        CurrentUser user = new CurrentUser(UUID.randomUUID(), UUID.randomUUID(), "admin@example.com", Role.TENANT_ADMIN);
        UUID documentId = UUID.randomUUID();
        when(currentUserResolver.fromJwt(any())).thenReturn(user);

        mockMvc.perform(delete("/documents/{documentId}", documentId))
                .andExpect(status().isNoContent());

        verify(documentService).delete(user, documentId);
    }
}
