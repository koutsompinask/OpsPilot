package com.opspilot.aiorchestrator.controller;

import com.opspilot.aiorchestrator.dto.ChatAskRequest;
import com.opspilot.aiorchestrator.dto.ChatAskResponse;
import com.opspilot.aiorchestrator.security.CurrentUser;
import com.opspilot.aiorchestrator.security.CurrentUserResolver;
import com.opspilot.aiorchestrator.service.ChatService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
