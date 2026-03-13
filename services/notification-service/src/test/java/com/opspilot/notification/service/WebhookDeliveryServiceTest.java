package com.opspilot.notification.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.opspilot.notification.config.WebhookProperties;
import com.opspilot.notification.dto.NotificationEnvelope;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class WebhookDeliveryServiceTest {

    @Test
    void shouldSkipWhenDisabled() {
        WebhookProperties properties = new WebhookProperties();
        properties.setEnabled(false);
        WebhookDeliveryService service = new WebhookDeliveryService(RestClient.builder(), properties);

        assertDoesNotThrow(() -> service.deliver(new NotificationEnvelope("ticket.created", "req-1", Instant.now(), java.util.Map.of())));
    }
}
