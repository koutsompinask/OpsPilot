package com.opspilot.knowledgebase.exception;

import java.time.Instant;

public record ApiError(
        String code,
        String message,
        Instant timestamp,
        String path
) {
}
