package com.opspilot.assistant.dto;

import java.util.List;

public record ChatAskResponse(
        String answer,
        double confidence,
        List<ChatSourceResponse> sources,
        boolean ticketCreated
) {
}
