package com.opspilot.assistant.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatAskRequest(
        @NotBlank String question,
        Integer topK
) {
}
