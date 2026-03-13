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

@Service
public class WebhookDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(WebhookDeliveryService.class);

    private final RestClient restClient;
    private final WebhookProperties webhookProperties;

    public WebhookDeliveryService(RestClient.Builder builder, WebhookProperties webhookProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(webhookProperties.getConnectTimeoutMs());
        requestFactory.setReadTimeout(webhookProperties.getReadTimeoutMs());
        this.restClient = builder.requestFactory(requestFactory).build();
        this.webhookProperties = webhookProperties;
    }

    public void deliver(NotificationEnvelope envelope) {
        if (!webhookProperties.isEnabled()) {
            log.info("notification_webhook_skipped eventType={} reason=disabled", envelope.eventType());
            return;
        }

        try {
            RequestBodySpec requestSpec = restClient.post().uri(URI.create(webhookProperties.getUrl()));
            if (hasText(webhookProperties.getAuthHeaderName()) && hasText(webhookProperties.getAuthHeaderValue())) {
                requestSpec.header(webhookProperties.getAuthHeaderName(), webhookProperties.getAuthHeaderValue());
            }
            requestSpec.body(envelope)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new IllegalStateException("Webhook delivery failed with status " + res.getStatusCode());
                    })
                    .toBodilessEntity();
            log.info("notification_webhook_delivered eventType={} target={}", envelope.eventType(), webhookProperties.getUrl());
        } catch (Exception ex) {
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
