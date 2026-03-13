package com.opspilot.ticket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.opspilot.ticket.domain.entity.Role;
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
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketCreatedEventPublisher ticketCreatedEventPublisher;

    private TicketService ticketService;
    private CurrentUser adminUser;

    @BeforeEach
    void setUp() {
        ticketService = new TicketService(ticketRepository, ticketCreatedEventPublisher);
        adminUser = new CurrentUser(UUID.randomUUID(), UUID.randomUUID(), "admin@example.com", Role.TENANT_ADMIN);
    }

    @Test
    void createInternalShouldPublishEvent() {
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketResponse response = ticketService.createInternal(new InternalCreateTicketRequest(
                adminUser.tenantId(),
                adminUser.userId(),
                adminUser.email(),
                "What is the refund policy?",
                "I am not confident.",
                0.23,
                0,
                "Auto-created"
        ), "request-123");

        assertEquals(TicketOrigin.CHAT_LOW_CONFIDENCE, response.origin());
        assertEquals(TicketStatus.OPEN, response.status());
        verify(ticketCreatedEventPublisher).publish(any(Ticket.class));
    }

    @Test
    void createManualShouldNormalizeSourceCountAndPublishEvent() {
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);

        TicketResponse response = ticketService.createManual(adminUser, new CreateTicketRequest(
                "Need a human follow-up",
                "Assistant answer",
                0.31,
                null,
                "Manual escalation"
        ));

        verify(ticketRepository).save(ticketCaptor.capture());
        verify(ticketCreatedEventPublisher).publish(ticketCaptor.getValue());
        assertEquals(0, ticketCaptor.getValue().getSourceCount());
        assertEquals("Need a human follow-up", response.question());
    }

    @Test
    void updateStatusShouldRejectMissingTicket() {
        when(ticketRepository.findByIdAndTenantId(any(UUID.class), any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> ticketService.updateStatus(
                adminUser,
                UUID.randomUUID(),
                new UpdateTicketStatusRequest(TicketStatus.RESOLVED)
        ));
    }

    @Test
    void createManualShouldRejectInvalidConfidence() {
        assertThrows(BadRequestException.class, () -> ticketService.createManual(adminUser, new CreateTicketRequest(
                "Need help",
                null,
                1.5,
                0,
                null
        )));
    }

    @Test
    void listShouldMapTenantTickets() {
        Ticket ticket = new Ticket();
        ticket.setId(UUID.randomUUID());
        ticket.setTenantId(adminUser.tenantId());
        ticket.setCreatedByUserId(adminUser.userId());
        ticket.setCreatedByEmail(adminUser.email());
        ticket.setOrigin(TicketOrigin.MANUAL);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setQuestion("Question");
        ticket.setAnswer("Answer");
        ticket.setConfidence(0.45);
        ticket.setSourceCount(2);
        ticket.setNotes("Notes");
        ticket.setCreatedRequestId("req");
        ticket.setId(ticket.getId());
        when(ticketRepository.findByTenantIdOrderByCreatedAtDesc(adminUser.tenantId())).thenReturn(List.of(ticket));

        List<TicketResponse> responses = ticketService.list(adminUser);

        assertEquals(1, responses.size());
        assertEquals(TicketOrigin.MANUAL, responses.getFirst().origin());
    }
}
