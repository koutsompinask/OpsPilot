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
import org.springframework.stereotype.Service;

@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);
    private final TicketRepository ticketRepository;
    private final TicketCreatedEventPublisher ticketCreatedEventPublisher;

    public TicketService(TicketRepository ticketRepository, TicketCreatedEventPublisher ticketCreatedEventPublisher) {
        this.ticketRepository = ticketRepository;
        this.ticketCreatedEventPublisher = ticketCreatedEventPublisher;
    }

    public List<TicketResponse> list(CurrentUser currentUser) {
        log.info("ticket_list_requested tenantId={} userId={}", currentUser.tenantId(), currentUser.userId());
        List<TicketResponse> tickets = ticketRepository.findByTenantIdOrderByCreatedAtDesc(currentUser.tenantId())
                .stream()
                .map(TicketResponse::fromEntity)
                .toList();
        log.info("ticket_list_succeeded tenantId={} userId={} ticketCount={}", currentUser.tenantId(), currentUser.userId(), tickets.size());
        return tickets;
    }

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

    @Transactional
    public TicketResponse createInternal(InternalCreateTicketRequest request, String requestId) {
        validateConfidence(request.confidence());
        int sourceCount = normalizeSourceCount(request.sourceCount());
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

    @Transactional
    public TicketResponse updateStatus(CurrentUser currentUser, UUID ticketId, UpdateTicketStatusRequest request) {
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
