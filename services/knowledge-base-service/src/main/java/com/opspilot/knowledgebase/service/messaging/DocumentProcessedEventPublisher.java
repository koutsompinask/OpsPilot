package com.opspilot.knowledgebase.service.messaging;

import com.opspilot.knowledgebase.domain.entity.Document;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class DocumentProcessedEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessedEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final MessagingProperties messagingProperties;

    public DocumentProcessedEventPublisher(RabbitTemplate rabbitTemplate, MessagingProperties messagingProperties) {
        this.rabbitTemplate = rabbitTemplate;
        this.messagingProperties = messagingProperties;
    }

    public void publish(Document document, int chunkCount, String requestId) {
        if (!messagingProperties.isEnabled()) {
            return;
        }

        DocumentProcessedEvent event = new DocumentProcessedEvent(
                requestId,
                document.getTenantId(),
                document.getId(),
                chunkCount,
                Instant.now()
        );

        rabbitTemplate.convertAndSend(
                messagingProperties.getDocumentProcessedExchange(),
                messagingProperties.getDocumentProcessedRoutingKey(),
                event
        );

        log.info(
                "knowledge_document_processed_event_published exchange={} routingKey={} documentId={} tenantId={} chunkCount={} requestId={}",
                messagingProperties.getDocumentProcessedExchange(),
                messagingProperties.getDocumentProcessedRoutingKey(),
                document.getId(),
                document.getTenantId(),
                chunkCount,
                requestId
        );
    }

    public record DocumentProcessedEvent(
            String requestId,
            UUID tenantId,
            UUID documentId,
            int chunkCount,
            Instant processedAt
    ) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
    }
}
