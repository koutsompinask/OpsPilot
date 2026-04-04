package com.opspilot.ticket.domain.entity;

/**
 * Describes how a support ticket was created.
 *
 * <p>The origin is recorded at creation time and is immutable thereafter. It is included in
 * the {@code ticket.created} event payload so that downstream consumers (e.g. the
 * notification-service) can tailor webhook messages based on how the ticket was raised.</p>
 */
public enum TicketOrigin {

    /**
     * The ticket was created automatically by the assistant-service because the chat response's
     * confidence score fell below the configured escalation threshold. The question, answer, and
     * confidence score are taken directly from the low-confidence chat exchange.
     */
    CHAT_LOW_CONFIDENCE,

    /**
     * The ticket was created manually by a tenant admin via the public API.
     */
    MANUAL
}
