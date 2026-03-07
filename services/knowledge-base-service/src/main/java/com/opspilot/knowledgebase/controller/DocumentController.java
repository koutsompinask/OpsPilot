package com.opspilot.knowledgebase.controller;

import com.opspilot.knowledgebase.dto.DocumentResponse;
import com.opspilot.knowledgebase.util.logging.RequestCorrelation;
import com.opspilot.knowledgebase.security.CurrentUser;
import com.opspilot.knowledgebase.security.CurrentUserResolver;
import com.opspilot.knowledgebase.service.DocumentService;
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

@RestController
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final CurrentUserResolver currentUserResolver;

    public DocumentController(DocumentService documentService, CurrentUserResolver currentUserResolver) {
        this.documentService = documentService;
        this.currentUserResolver = currentUserResolver;
    }

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

    @GetMapping
    public List<DocumentResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return documentService.list(currentUserResolver.fromJwt(jwt));
    }

    @GetMapping("/{documentId}")
    public DocumentResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID documentId) {
        return documentService.get(currentUserResolver.fromJwt(jwt), documentId);
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID documentId) {
        documentService.delete(currentUserResolver.fromJwt(jwt), documentId);
        return ResponseEntity.noContent().build();
    }
}
