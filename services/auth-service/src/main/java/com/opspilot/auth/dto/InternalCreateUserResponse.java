package com.opspilot.auth.dto;

import com.opspilot.auth.domain.entity.Role;
import java.util.UUID;

/**
 * Response payload returned by the internal {@code POST /internal/auth/users} endpoint after
 * successfully creating an auth user record.
 *
 * <p>Echoes the identifiers and role of the newly created user back to the calling service
 * (tenant-service) for confirmation.</p>
 *
 * @param userId   the UUID of the created auth user
 * @param tenantId the UUID of the tenant the user was registered under
 * @param email    the normalised email stored for the user
 * @param role     the role assigned to the user
 */
public record InternalCreateUserResponse(
        UUID userId,
        UUID tenantId,
        String email,
        Role role
) {
}
