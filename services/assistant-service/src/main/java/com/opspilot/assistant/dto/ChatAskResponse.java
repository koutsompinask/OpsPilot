package com.opspilot.assistant.dto;

import java.util.List;

public record ChatAskResponse(
        String answer,
        String reasoningSummary,
        double confidence,
        List<ChatSourceResponse> sources,
        List<ChatEvidenceResponse> evidence,
        String answerMode,
        boolean ticketCreated
) {
}
