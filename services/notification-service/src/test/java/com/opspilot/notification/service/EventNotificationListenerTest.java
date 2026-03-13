package com.opspilot.notification.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.opspilot.notification.config.MessagingProperties;
import com.opspilot.notification.dto.DocumentProcessedEvent;
import com.opspilot.notification.dto.NotificationEnvelope;
import com.opspilot.notification.dto.TicketCreatedEvent;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventNotificationListenerTest {

    @Mock
    private WebhookDeliveryService webhookDeliveryService;

    @Mock
    private MessagingProperties messagingProperties;

    @Test
    void shouldDeliverTicketCreatedEvent() {
        when(messagingProperties.isEnabled()).thenReturn(true);
        EventNotificationListener listener = new EventNotificationListener(webhookDeliveryService, messagingProperties);
        TicketCreatedEvent event = new TicketCreatedEvent(
                "req-1",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "user@example.com",
                "CHAT_LOW_CONFIDENCE",
                "OPEN",
                0.23,
                0,
                "Question excerpt",
                Instant.now()
        );

        listener.onTicketCreated(event);

        ArgumentCaptor<NotificationEnvelope> captor = ArgumentCaptor.forClass(NotificationEnvelope.class);
        verify(webhookDeliveryService).deliver(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("ticket.created", captor.getValue().eventType());
    }

    @Test
    void shouldDeliverDocumentProcessedEvent() {
        when(messagingProperties.isEnabled()).thenReturn(true);
        EventNotificationListener listener = new EventNotificationListener(webhookDeliveryService, messagingProperties);
        DocumentProcessedEvent event = new DocumentProcessedEvent("req-2", UUID.randomUUID(), UUID.randomUUID(), 4, Instant.now());

        listener.onDocumentProcessed(event);

        verify(webhookDeliveryService).deliver(org.mockito.ArgumentMatchers.any(NotificationEnvelope.class));
    }
}
