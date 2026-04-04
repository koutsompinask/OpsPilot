package com.opspilot.ticket.domain.entity;

/**
 * Role assigned to an authenticated user within their tenant.
 *
 * <p>Roles are extracted from the JWT's {@code role} claim and used to gate write operations.
 * Only {@link #TENANT_ADMIN} users may create manual tickets or update ticket status.</p>
 */
public enum Role {
    /** A tenant administrator with full read/write access to their tenant's tickets. */
    TENANT_ADMIN,

    /** A regular tenant member with read-only access to tickets. */
    TENANT_MEMBER
}
