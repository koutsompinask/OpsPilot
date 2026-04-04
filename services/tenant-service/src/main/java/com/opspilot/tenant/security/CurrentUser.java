package com.opspilot.tenant.security;

import com.opspilot.tenant.domain.entity.Role;
import java.util.UUID;

/**
 * Immutable representation of the authenticated caller, extracted from a validated JWT.
 *
 * <p>Instances are created by {@link CurrentUserResolver#fromJwt} and passed into service
 * methods so that business logic can make authorization decisions without touching the
 * raw JWT again. Every field maps directly to a standard JWT claim issued by auth-service:
 * {@code sub} → {@code userId}, {@code tenant_id} → {@code tenantId}, {@code email} → {@code email},
 * {@code role} → {@code role}.
 */
public record CurrentUser(UUID userId, UUID tenantId, String email, Role role) {

    /**
     * Returns {@code true} when the caller holds the {@code TENANT_ADMIN} role.
     *
     * <p>Used throughout the controllers to gate admin-only operations such as
     * listing users or updating tenant settings.
     *
     * @return {@code true} if the caller is a tenant administrator
     */
    public boolean isAdmin() {
        return role == Role.TENANT_ADMIN;
    }
}
