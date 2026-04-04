package com.opspilot.assistant.controller;

import com.opspilot.assistant.dto.ChatAskRequest;
import com.opspilot.assistant.dto.ChatAskResponse;
import com.opspilot.assistant.security.CurrentUser;
import com.opspilot.assistant.security.CurrentUserResolver;
import com.opspilot.assistant.service.ChatService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller that exposes the RAG-powered chat endpoint.
 *
 * <p>Accepts natural-language questions from authenticated users and delegates to
 * {@link ChatService} for full RAG pipeline execution: embedding, hybrid retrieval,
 * reranking, answer generation, and optional ticket escalation.</p>
 */
@RestController
@RequestMapping("/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final CurrentUserResolver currentUserResolver;
    private final ChatService chatService;

    public ChatController(CurrentUserResolver currentUserResolver, ChatService chatService) {
        this.currentUserResolver = currentUserResolver;
        this.chatService = chatService;
    }

    /**
     * Handles a natural-language question and returns an AI-generated answer with
     * supporting evidence chunks.
     *
     * <p>If the generated answer falls below the configured confidence threshold,
     * a support ticket is automatically created on behalf of the user and the response
     * includes {@code ticketCreated: true}.</p>
     *
     * @param request the question payload, including optional {@code topK} override
     * @param jwt     the validated Bearer JWT injected by Spring Security
     * @return the answer, confidence score, source list, evidence chunks, and escalation flag
     */
    @PostMapping("/ask")
    public ChatAskResponse ask(@Valid @RequestBody ChatAskRequest request, @AuthenticationPrincipal Jwt jwt) {
        long startedAt = System.currentTimeMillis();
        CurrentUser user = currentUserResolver.fromJwt(jwt);
        log.info("ai_chat_http_request_received method=POST path=/chat/ask tenantId={} userId={}", user.tenantId(), user.userId());
        ChatAskResponse response = chatService.ask(user, request.question(), request.topK());
        log.info(
                "ai_chat_http_request_completed method=POST path=/chat/ask tenantId={} userId={} status=200 durationMs={}",
                user.tenantId(),
                user.userId(),
                System.currentTimeMillis() - startedAt
        );
        return response;
    }
}
