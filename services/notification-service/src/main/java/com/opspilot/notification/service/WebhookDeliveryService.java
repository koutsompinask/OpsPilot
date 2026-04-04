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
 * Delivers a {@link NotificationEnvelope} to the configured webhook URL via HTTP POST.
 *
 * <p>Delivery is <em>best-effort</em>: if the HTTP call fails for any reason (connection
 * refused, timeout, non-2xx response) the exception is caught, logged, and swallowed. There
 * is intentionally no retry logic and no dead-letter queue — the service is stateless and
 * keeping it that way avoids the complexity of durable retry state while still providing
 * timely delivery under normal conditions. Operators can monitor delivery failures through
 * the structured error log ({@code notification_webhook_delivery_failed}).</p>
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
     * Sends the given envelope to the webhook target as a JSON HTTP POST.
     *
     * <p>Delivery is best-effort: any exception (network error, timeout, HTTP error status) is
     * caught and logged without propagation. This prevents a transient webhook failure from
     * causing unnecessary AMQP message redelivery, which would not resolve the underlying issue.</p>
     *
     * @param envelope the notification payload to deliver, including the event type and timestamp
     */
    public void deliver(NotificationEnvelope envelope) {
        if (!webhookProperties.isEnabled()) {
            log.info("notification_webhook_skipped eventType={} reason=disabled", envelope.eventType());
            return;
        }

        try {
            RequestBodySpec requestSpec = restClient.post().uri(URI.create(webhookProperties.getUrl()));
            // Add the optional shared-secret header only when both name and value are configured
            if (hasText(webhookProperties.getAuthHeaderName()) && hasText(webhookProperties.getAuthHeaderValue())) {
                requestSpec.header(webhookProperties.getAuthHeaderName(), webhookProperties.getAuthHeaderValue());
            }
            requestSpec.body(envelope)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        // Convert HTTP error responses into an exception so the catch block can log them uniformly
                        throw new IllegalStateException("Webhook delivery failed with status " + res.getStatusCode());
                    })
                    .toBodilessEntity();
            log.info("notification_webhook_delivered eventType={} target={}", envelope.eventType(), webhookProperties.getUrl());
        } catch (Exception ex) {
            // Best-effort: log the failure and continue — do not rethrow, as that would cause
            // the AMQP listener to NACK the message and trigger redelivery, which cannot help
            // if the webhook host itself is down or returning errors
            log.error(
                    "notification_webhook_delivery_failed eventType={} target={} reason={}",
                    envelope.eventType(),
                    webhookProperties.getUrl(),
                    ex.getMessage(),
                    ex
            );
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
