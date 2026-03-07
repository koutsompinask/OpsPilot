package com.opspilot.tenant.logging;

import java.util.UUID;

public final class RequestCorrelation {

    public static final String HEADER_NAME = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    private RequestCorrelation() {
    }

    public static String normalizeOrGenerate(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return requestId;
    }
}
