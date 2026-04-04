package com.opspilot.ticket.security;

import com.opspilot.ticket.domain.entity.Role;
import java.util.UUID;

/**
 * Immutable value object representing the authenticated principal for the current request.
 *
 * <p>Instances are created by {@link CurrentUserResolver} by extracting claims from the validated
 * JWT. All ticket operations are scoped to the caller's {@code tenantId}, preventing cross-tenant
 * data access.</p>
 */
public record CurrentUser(UUID userId, UUID tenantId, String email, Role role) {

    /**
     * Returns {@code true} if the authenticated user holds the {@link Role#TENANT_ADMIN} role.
     *
     * <p>Admin status is required to create manual tickets and to update ticket status. This
     * centralises the role check so that callers do not need to reference {@link Role} directly.</p>
     *
     * @return {@code true} when the user is a tenant admin, {@code false} otherwise
     */
    public boolean isAdmin() {
        return role == Role.TENANT_ADMIN;
    }
}
