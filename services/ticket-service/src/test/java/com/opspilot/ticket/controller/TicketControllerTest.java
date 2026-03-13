package com.opspilot.ticket.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.opspilot.ticket.domain.entity.Role;
import com.opspilot.ticket.domain.entity.TicketOrigin;
import com.opspilot.ticket.domain.entity.TicketStatus;
import com.opspilot.ticket.dto.TicketResponse;
import com.opspilot.ticket.exception.GlobalExceptionHandler;
import com.opspilot.ticket.security.CurrentUser;
import com.opspilot.ticket.security.CurrentUserResolver;
import com.opspilot.ticket.service.TicketService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = TicketController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TicketService ticketService;

    @MockBean
    private CurrentUserResolver currentUserResolver;

    @Test
    void listShouldReturnTicketsForTenantMember() throws Exception {
        CurrentUser user = new CurrentUser(UUID.randomUUID(), UUID.randomUUID(), "member@example.com", Role.TENANT_MEMBER);
        when(currentUserResolver.fromJwt(any())).thenReturn(user);
        when(ticketService.list(eq(user))).thenReturn(List.of(ticketResponse(user.tenantId(), user.userId(), TicketOrigin.CHAT_LOW_CONFIDENCE)));

        mockMvc.perform(get("/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].origin").value("CHAT_LOW_CONFIDENCE"))
                .andExpect(jsonPath("$[0].status").value("OPEN"));
    }

    @Test
    void createShouldRejectTenantMember() throws Exception {
        CurrentUser user = new CurrentUser(UUID.randomUUID(), UUID.randomUUID(), "member@example.com", Role.TENANT_MEMBER);
        when(currentUserResolver.fromJwt(any())).thenReturn(user);

        mockMvc.perform(post("/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"Need support\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void updateStatusShouldRejectTenantMember() throws Exception {
        CurrentUser user = new CurrentUser(UUID.randomUUID(), UUID.randomUUID(), "member@example.com", Role.TENANT_MEMBER);
        when(currentUserResolver.fromJwt(any())).thenReturn(user);

        mockMvc.perform(patch("/tickets/{ticketId}/status", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private TicketResponse ticketResponse(UUID tenantId, UUID userId, TicketOrigin origin) {
        return new TicketResponse(
                UUID.randomUUID(),
                tenantId,
                userId,
                "member@example.com",
                origin,
                TicketStatus.OPEN,
                "Need support",
                "Low confidence",
                0.42,
                1,
                "Auto-created",
                Instant.now(),
                Instant.now()
        );
    }
}
