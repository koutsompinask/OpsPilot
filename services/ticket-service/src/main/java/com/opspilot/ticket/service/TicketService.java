package com.opspilot.ticket.service;

import com.opspilot.ticket.domain.entity.Ticket;
import com.opspilot.ticket.domain.entity.TicketOrigin;
import com.opspilot.ticket.domain.entity.TicketStatus;
import com.opspilot.ticket.dto.CreateTicketRequest;
import com.opspilot.ticket.dto.InternalCreateTicketRequest;
import com.opspilot.ticket.dto.TicketResponse;
import com.opspilot.ticket.dto.UpdateTicketStatusRequest;
import com.opspilot.ticket.exception.BadRequestException;
import com.opspilot.ticket.exception.NotFoundException;
import com.opspilot.ticket.repository.TicketRepository;
import com.opspilot.ticket.security.CurrentUser;
import com.opspilot.ticket.service.messaging.TicketCreatedEventPublisher;
import com.opspilot.ticket.util.logging.RequestCorrelation;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Core business logic for the ticket-service.
 *
 * <p>Handles ticket listing, manual creation by tenant admins, internal creation triggered by
 * the assistant-service (auto-escalation), and status updates. After every successful ticket
 * creation the service publishes a {@code ticket.created} event to RabbitMQ via
 * {@link TicketCreatedEventPublisher} so that the notification-service can deliver webhooks.</p>
 *
 * <p>All data access is tenant-scoped: queries always filter by {@code tenantId} to prevent
 * cross-tenant data leakage.</p>
 */
@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);
    private final TicketRepository ticketRepository;
    private final TicketCreatedEventPublisher ticketCreatedEventPublisher;

    public TicketService(TicketRepository ticketRepository, TicketCreatedEventPublisher ticketCreatedEventPublisher) {
        this.ticketRepository = ticketRepository;
        this.ticketCreatedEventPublisher = ticketCreatedEventPublisher;
    }

    /**
     * Returns a page of tickets for the caller's tenant, ordered by the sort specified in {@code pageable}.
     *
     * @param currentUser the authenticated principal used to scope the query to a single tenant
     * @param pageable    pagination and sort parameters supplied by the caller
     * @return a page of ticket responses
     */
    public Page<TicketResponse> list(CurrentUser currentUser, Pageable pageable) {
        log.info("ticket_list_requested tenantId={} userId={} page={} size={}", currentUser.tenantId(), currentUser.userId(), pageable.getPageNumber(), pageable.getPageSize());
        Page<TicketResponse> tickets = ticketRepository.findByTenantId(currentUser.tenantId(), pageable)
                .map(TicketResponse::fromEntity);
        log.info("ticket_list_succeeded tenantId={} userId={} ticketCount={} totalElements={}", currentUser.tenantId(), currentUser.userId(), tickets.getNumberOfElements(), tickets.getTotalElements());
        return tickets;
    }

    /**
     * Creates a ticket manually on behalf of an authenticated tenant admin.
     *
     * <p>The caller's identity is used to populate {@code tenantId}, {@code createdByUserId}, and
     * {@code createdByEmail}. The origin is set to {@link TicketOrigin#MANUAL}. The correlation ID
     * is taken from the current MDC context so the ticket is traceable to the HTTP request that
     * triggered it. A {@code ticket.created} event is published after a successful save.</p>
     *
     * @param currentUser the authenticated admin creating the ticket
     * @param request     the validated ticket creation payload
     * @return the persisted ticket
     * @throws com.opspilot.ticket.exception.BadRequestException if confidence is outside [0, 1]
     *         or sourceCount is negative
     */
    @Transactional
    public TicketResponse createManual(CurrentUser currentUser, CreateTicketRequest request) {
        validateConfidence(request.confidence());
        int sourceCount = normalizeSourceCount(request.sourceCount());
        Ticket ticket = buildTicket(
                UUID.randomUUID(),
                currentUser.tenantId(),
                currentUser.userId(),
                normalizeEmail(currentUser.email()),
                TicketOrigin.MANUAL,
                normalizeQuestion(request.question()),
                normalizeOptional(request.answer()),
                request.confidence(),
                sourceCount,
                normalizeOptional(request.notes()),
                RequestCorrelation.currentRequestId()
        );
        ticketRepository.save(ticket);
        log.info(
                "ticket_created_manual ticketId={} tenantId={} actorUserId={} sourceCount={}",
                ticket.getId(),
                ticket.getTenantId(),
                currentUser.userId(),
                ticket.getSourceCount()
        );
        ticketCreatedEventPublisher.publish(ticket);
        return TicketResponse.fromEntity(ticket);
    }

    /**
     * Creates a ticket on behalf of the assistant-service during auto-escalation.
     *
     * <p>This path is triggered when the assistant-service determines that a chat response's
     * confidence score is below the escalation threshold and calls the internal endpoint. The
     * origin is set to {@link TicketOrigin#CHAT_LOW_CONFIDENCE} to distinguish auto-escalated
     * tickets from manually created ones. The originating request correlation ID is preserved
     * so the ticket can be traced back to the specific chat session.</p>
     *
     * @param request   the validated payload provided by the assistant-service, including tenant
     *                  and user context that the assistant-service extracted from the user's JWT
     * @param requestId the correlation ID forwarded from the originating chat request; a new UUID
     *                  is generated if absent
     * @return the persisted ticket
     * @throws com.opspilot.ticket.exception.BadRequestException if confidence is outside [0, 1]
     *         or sourceCount is negative
     */
    @Transactional
    public TicketResponse createInternal(InternalCreateTicketRequest request, String requestId) {
        validateConfidence(request.confidence());
        int sourceCount = normalizeSourceCount(request.sourceCount());
        // Normalise the forwarded correlation ID so it fits the column length constraint (128 chars)
        String normalizedRequestId = RequestCorrelation.normalizeOrGenerate(requestId);
        Ticket ticket = buildTicket(
                UUID.randomUUID(),
                request.tenantId(),
                request.createdByUserId(),
                normalizeEmail(request.createdByEmail()),
                TicketOrigin.CHAT_LOW_CONFIDENCE,
                normalizeQuestion(request.question()),
                normalizeOptional(request.answer()),
                request.confidence(),
                sourceCount,
                normalizeOptional(request.notes()),
                normalizedRequestId
        );
        ticketRepository.save(ticket);
        log.info(
                "ticket_created_internal ticketId={} tenantId={} createdByUserId={} confidence={} sourceCount={} requestId={}",
                ticket.getId(),
                ticket.getTenantId(),
                ticket.getCreatedByUserId(),
                ticket.getConfidence(),
                ticket.getSourceCount(),
                normalizedRequestId
        );
        ticketCreatedEventPublisher.publish(ticket);
        return TicketResponse.fromEntity(ticket);
    }

    /**
     * Updates the status of a ticket within the caller's tenant.
     *
     * <p>The lookup uses both {@code ticketId} and {@code tenantId} to prevent a tenant admin
     * from modifying tickets belonging to another tenant even if they know the ticket's UUID.
     * There are no guards on which status transitions are valid — any transition from any status
     * to any other status is permitted; workflow enforcement is left to the UI layer.</p>
     *
     * @param currentUser the authenticated admin performing the update
     * @param ticketId    the UUID of the ticket to update
     * @param request     the new status, validated before processing
     * @return the updated ticket
     * @throws NotFoundException if no ticket with the given ID exists within the caller's tenant
     */
    @Transactional
    public TicketResponse updateStatus(CurrentUser currentUser, UUID ticketId, UpdateTicketStatusRequest request) {
        // Lookup scoped to tenantId to enforce tenant isolation on write operations
        Ticket ticket = ticketRepository.findByIdAndTenantId(ticketId, currentUser.tenantId())
                .orElseThrow(() -> new NotFoundException("Ticket not found"));
        TicketStatus previousStatus = ticket.getStatus();
        ticket.setStatus(request.status());
        ticketRepository.save(ticket);
        log.info(
                "ticket_status_updated ticketId={} tenantId={} actorUserId={} previousStatus={} newStatus={}",
                ticket.getId(),
                ticket.getTenantId(),
                currentUser.userId(),
                previousStatus,
                ticket.getStatus()
        );
        return TicketResponse.fromEntity(ticket);
    }

    private Ticket buildTicket(
            UUID ticketId,
            UUID tenantId,
            UUID createdByUserId,
            String createdByEmail,
            TicketOrigin origin,
            String question,
            String answer,
            Double confidence,
            int sourceCount,
            String notes,
            String requestId
    ) {
        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setTenantId(tenantId);
        ticket.setCreatedByUserId(createdByUserId);
        ticket.setCreatedByEmail(createdByEmail);
        ticket.setOrigin(origin);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setQuestion(question);
        ticket.setAnswer(answer);
        ticket.setConfidence(confidence);
        ticket.setSourceCount(sourceCount);
        ticket.setNotes(notes);
        ticket.setCreatedRequestId(requestId);
        return ticket;
    }

    private String normalizeQuestion(String question) {
        String normalized = normalizeOptional(question);
        if (normalized == null || normalized.isBlank()) {
            throw new BadRequestException("Question is required");
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeEmail(String email) {
        return email.toLowerCase(Locale.ROOT).trim();
    }

    private void validateConfidence(Double confidence) {
        if (confidence == null) {
            return;
        }
        if (confidence < 0.0 || confidence > 1.0) {
            throw new BadRequestException("confidence must be between 0 and 1");
        }
    }

    private int normalizeSourceCount(Integer sourceCount) {
        if (sourceCount == null) {
            return 0;
        }
        if (sourceCount < 0) {
            throw new BadRequestException("sourceCount must be greater than or equal to 0");
        }
        return sourceCount;
    }
}
