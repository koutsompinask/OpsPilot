package com.opspilot.notification.service;

import com.opspilot.notification.config.WebhookProperties;
import com.opspilot.notification.dto.NotificationEnvelope;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestBodySpec;

/**
 * Delivers a {@link NotificationEnvelope} to the configured webhook URL via HTTP POST,
 * with configurable exponential-backoff retry.
 *
 * <p>On failure (connection refused, timeout, non-2xx response), the delivery is retried up to
 * {@code notification.webhook.maxRetries} times (default 3) with an initial delay of
 * {@code notification.webhook.retryInitialDelayMs} (default 1 s) that doubles on each attempt.
 * If all attempts fail the final exception is logged and swallowed so that the AMQP listener
 * is not forced to NACK the message — that would only cause immediate redelivery, which
 * cannot help when the webhook host is persistently down.</p>
 *
 * <p>An optional shared-secret auth header is injected when both
 * {@code notification.webhook.authHeaderName} and {@code notification.webhook.authHeaderValue}
 * are configured, allowing the webhook receiver to authenticate incoming calls.</p>
 */
@Service
public class WebhookDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(WebhookDeliveryService.class);

    private final RestClient restClient;
    private final WebhookProperties webhookProperties;

    /**
     * Constructs the service and builds a {@link RestClient} with per-request timeouts.
     *
     * @param builder           Spring-provided {@link RestClient.Builder} (pre-configured with defaults)
     * @param webhookProperties timeout and target URL configuration
     */
    public WebhookDeliveryService(RestClient.Builder builder, WebhookProperties webhookProperties) {
        // Apply explicit connect and read timeouts so a slow or unresponsive webhook
        // does not block the AMQP listener thread indefinitely
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(webhookProperties.getConnectTimeoutMs());
        requestFactory.setReadTimeout(webhookProperties.getReadTimeoutMs());
        this.restClient = builder.requestFactory(requestFactory).build();
        this.webhookProperties = webhookProperties;
    }

    /**
     * Sends the given envelope to the webhook target as a JSON HTTP POST, retrying on failure
     * with exponential backoff up to {@code maxRetries} additional attempts.
     *
     * <p>After all retries are exhausted the final error is logged and swallowed. This prevents
     * the AMQP listener from NACKing the message and triggering immediate redelivery, which
     * cannot help when the webhook host itself is down.</p>
     *
     * @param envelope the notification payload to deliver, including the event type and timestamp
     */
    public void deliver(NotificationEnvelope envelope) {
        if (!webhookProperties.isEnabled()) {
            log.info("notification_webhook_skipped eventType={} reason=disabled", envelope.eventType());
            return;
        }

        int maxRetries = webhookProperties.getMaxRetries();
        long delayMs = webhookProperties.getRetryInitialDelayMs();
        Exception lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                attemptDelivery(envelope);
                log.info("notification_webhook_delivered eventType={} target={} attempt={}", envelope.eventType(), webhookProperties.getUrl(), attempt + 1);
                return;
            } catch (Exception ex) {
                lastException = ex;
                if (attempt < maxRetries) {
                    log.warn(
                            "notification_webhook_delivery_retrying eventType={} target={} attempt={} maxRetries={} delayMs={} reason={}",
                            envelope.eventType(), webhookProperties.getUrl(), attempt + 1, maxRetries, delayMs, ex.getMessage()
                    );
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    // Exponential backoff: double the delay on each retry
                    delayMs *= 2;
                }
            }
        }

        // All attempts exhausted — log and swallow so the AMQP listener does not NACK
        log.error(
                "notification_webhook_delivery_failed eventType={} target={} attempts={} reason={}",
                envelope.eventType(),
                webhookProperties.getUrl(),
                maxRetries + 1,
                lastException != null ? lastException.getMessage() : "unknown",
                lastException
        );
    }

    /**
     * Performs a single HTTP POST attempt.
     *
     * @param envelope the payload to send
     * @throws Exception if the request fails or returns an error status
     */
    private void attemptDelivery(NotificationEnvelope envelope) {
        RequestBodySpec requestSpec = restClient.post().uri(URI.create(webhookProperties.getUrl()));
        // Add the optional shared-secret header only when both name and value are configured
        if (hasText(webhookProperties.getAuthHeaderName()) && hasText(webhookProperties.getAuthHeaderValue())) {
            requestSpec.header(webhookProperties.getAuthHeaderName(), webhookProperties.getAuthHeaderValue());
        }
        requestSpec.body(envelope)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    // Convert HTTP error responses into an exception so the retry loop can catch them uniformly
                    throw new IllegalStateException("Webhook delivery failed with status " + res.getStatusCode());
                })
                .toBodilessEntity();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
