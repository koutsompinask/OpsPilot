package com.opspilot.notification.dto;

import java.time.Instant;

/**
 * Uniform wrapper sent as the JSON body of every outbound webhook POST.
 *
 * <p>All event types are normalised into this single envelope structure before delivery so
 * that webhook receivers can implement a single, stable contract regardless of which internal
 * event triggered the notification. The {@code eventType} discriminator tells the receiver
 * which concrete type to expect in {@code payload}, and {@code occurredAt} provides a
 * source-of-truth timestamp independent of delivery latency.</p>
 *
 * @param eventType  string discriminator identifying the event (e.g. {@code ticket.created},
 *                   {@code document.processed})
 * @param requestId  correlation ID from the originating request, suitable for log tracing
 * @param occurredAt timestamp at which the underlying domain event occurred (not delivery time)
 * @param payload    event-specific data; structure depends on {@code eventType}
 */
public record NotificationEnvelope(
        String eventType,
        String requestId,
        Instant occurredAt,
        Object payload
) {
}
