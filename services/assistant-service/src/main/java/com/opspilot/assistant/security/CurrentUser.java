package com.opspilot.assistant.security;

import com.opspilot.assistant.domain.entity.Role;
import java.util.UUID;

/**
 * Immutable representation of the authenticated user attached to the current request.
 *
 * <p>Populated by {@link CurrentUserResolver} from the validated JWT and passed through
 * the service layer to enforce per-tenant data isolation and role-based access control.
 * All tenant-scoped repository queries receive {@code tenantId} from this record to
 * prevent cross-tenant data leakage.</p>
 */
public record CurrentUser(UUID userId, UUID tenantId, String email, Role role) {

    /**
     * Returns {@code true} if this user holds the {@code TENANT_ADMIN} role, which is
     * required for document upload and deletion operations.
     */
    public boolean isAdmin() {
        return role == Role.TENANT_ADMIN;
    }
}
