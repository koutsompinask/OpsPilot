package com.opspilot.ticket.domain.entity;

/**
 * Lifecycle status of a support ticket.
 *
 * <p>Status transitions are not strictly enforced at the service layer — any transition is
 * permitted. Workflow rules (e.g. preventing re-opening a resolved ticket) are enforced by
 * the UI. Only tenant admins may change ticket status.</p>
 */
public enum TicketStatus {
    /** The ticket has been raised and is awaiting attention. */
    OPEN,

    /** A tenant admin has begun working on the ticket. */
    IN_PROGRESS,

    /** The issue has been addressed and the ticket is closed. */
    RESOLVED
}
