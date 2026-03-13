package com.opspilot.ticket.repository;

import com.opspilot.ticket.domain.entity.Ticket;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    List<Ticket> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    Optional<Ticket> findByIdAndTenantId(UUID id, UUID tenantId);
}
