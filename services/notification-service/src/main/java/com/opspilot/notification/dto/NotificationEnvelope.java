package com.opspilot.notification.dto;

import java.time.Instant;

public record NotificationEnvelope(
        String eventType,
        String requestId,
        Instant occurredAt,
        Object payload
) {
}
