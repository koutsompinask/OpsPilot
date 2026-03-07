package com.opspilot.apigateway.logging;

import java.util.UUID;

public final class RequestCorrelation {

    public static final String HEADER_NAME = "X-Request-Id";

    private RequestCorrelation() {
    }

    public static String normalizeOrGenerate(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return requestId;
    }
}
