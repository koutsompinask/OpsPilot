package com.opspilot.assistant.controller;

import com.opspilot.assistant.dto.DocumentResponse;
import com.opspilot.assistant.util.logging.RequestCorrelation;
import com.opspilot.assistant.security.CurrentUser;
import com.opspilot.assistant.security.CurrentUserResolver;
import com.opspilot.assistant.service.DocumentService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for managing the tenant knowledge-base documents.
 *
 * <p>Upload, list, retrieve, and delete operations are all scoped to the authenticated
 * user's tenant. Mutation endpoints (upload, delete) are restricted to
 * {@code TENANT_ADMIN} users and are enforced inside {@link DocumentService}. Uploaded
 * documents are accepted immediately with HTTP 202 while ingestion (chunking and
 * embedding) proceeds asynchronously in the background.</p>
 */
@RestController
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final CurrentUserResolver currentUserResolver;

    public DocumentController(DocumentService documentService, CurrentUserResolver currentUserResolver) {
        this.documentService = documentService;
        this.currentUserResolver = currentUserResolver;
    }

    /**
     * Accepts a document upload, persists it to object storage, and schedules async ingestion.
     *
     * <p>Returns HTTP 202 Accepted immediately; the document status transitions from
     * {@code PROCESSING} to {@code READY} (or {@code FAILED}) once the background
     * ingestion pipeline completes.</p>
     *
     * @param jwt     the caller's validated Bearer JWT
     * @param file    the document file to ingest (only {@code .txt} and {@code .md} are supported)
     * @param request the raw HTTP request, used to propagate the correlation ID header
     * @return the persisted document metadata at 202 Accepted
     */
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<DocumentResponse> upload(
            @AuthenticationPrincipal Jwt jwt,
            @RequestPart("file") MultipartFile file,
            HttpServletRequest request
    ) {
        CurrentUser user = currentUserResolver.fromJwt(jwt);
        DocumentResponse response = documentService.create(user, file, request.getHeader(RequestCorrelation.HEADER_NAME));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Returns all documents belonging to the caller's tenant, ordered by creation time descending.
     *
     * @param jwt the caller's validated Bearer JWT
     * @return list of document metadata records for the tenant
     */
    @GetMapping
    public List<DocumentResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return documentService.list(currentUserResolver.fromJwt(jwt));
    }

    /**
     * Returns the metadata for a single document, scoped to the caller's tenant.
     *
     * @param jwt        the caller's validated Bearer JWT
     * @param documentId the UUID of the document to retrieve
     * @return the document metadata
     */
    @GetMapping("/{documentId}")
    public DocumentResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID documentId) {
        return documentService.get(currentUserResolver.fromJwt(jwt), documentId);
    }

    /**
     * Deletes a document along with all its chunks and its object-storage file.
     *
     * @param jwt        the caller's validated Bearer JWT (must be a tenant admin)
     * @param documentId the UUID of the document to delete
     * @return HTTP 204 No Content on success
     */
    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID documentId) {
        documentService.delete(currentUserResolver.fromJwt(jwt), documentId);
        return ResponseEntity.noContent().build();
    }
}
