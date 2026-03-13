package com.opspilot.notification.util.logging;

import java.util.UUID;

public final class RequestCorrelation {

    public static final String MDC_KEY = "requestId";

    private RequestCorrelation() {
    }

    public static String normalizeOrGenerate(String value) {
        if (value == null || value.isBlank()) {
            return UUID.randomUUID().toString();
        }
        String trimmed = value.trim();
        return trimmed.length() > 128 ? trimmed.substring(0, 128) : trimmed;
    }
}
