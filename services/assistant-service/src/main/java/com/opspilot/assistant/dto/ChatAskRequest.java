package com.opspilot.assistant.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /chat/ask}.
 *
 * @param question the user's question; must not be blank
 * @param topK     optional override for the number of chunks to retrieve; uses the service default when null
 */
public record ChatAskRequest(
        @NotBlank String question,
        Integer topK
) {
}
