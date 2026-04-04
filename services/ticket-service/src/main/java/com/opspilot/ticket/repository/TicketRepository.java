package com.opspilot.ticket.repository;

import com.opspilot.ticket.domain.entity.Ticket;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link Ticket} entities.
 *
 * <p>All custom query methods enforce tenant isolation by including {@code tenantId} as a
 * parameter. This prevents accidental cross-tenant data access if callers omit the scoping
 * filter.</p>
 */
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    /**
     * Returns all tickets belonging to the specified tenant, newest first.
     *
     * @param tenantId the tenant whose tickets to fetch
     * @return ordered list of tickets; empty if none exist for the tenant
     */
    List<Ticket> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    /**
     * Returns a page of tickets for a tenant; sort and page size are controlled by the caller via {@code pageable}.
     *
     * @param tenantId the tenant whose tickets to fetch
     * @param pageable pagination and sort parameters
     * @return a page of tickets
     */
    Page<Ticket> findByTenantId(UUID tenantId, Pageable pageable);

    /**
     * Looks up a ticket by its primary key within the bounds of a specific tenant.
     *
     * <p>The {@code tenantId} parameter is intentional: it ensures that a valid ticket UUID
     * belonging to a different tenant is treated as not found, providing tenant isolation on
     * single-resource lookups.</p>
     *
     * @param id       the ticket's primary key
     * @param tenantId the tenant that must own the ticket
     * @return an {@link Optional} containing the ticket, or empty if not found in that tenant
     */
    Optional<Ticket> findByIdAndTenantId(UUID id, UUID tenantId);
}
