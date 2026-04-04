package com.opspilot.auth.dto;

import com.opspilot.auth.domain.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Request payload for the internal {@code POST /internal/auth/users} endpoint.
 *
 * <p>Used by tenant-service to register a new member user's credentials in the auth-service.
 * The {@code userId} and {@code tenantId} are assigned by the caller so that both services
 * reference the same stable identifiers.</p>
 *
 * @param userId   the pre-assigned UUID for the new user; must match the ID used in tenant-service
 * @param tenantId the UUID of the tenant the user belongs to
 * @param email    the user's email address; must be unique across all auth users
 * @param password plaintext password; must be 8–128 characters
 * @param role     the role to assign to the user within their tenant
 */
public record InternalCreateUserRequest(
        @NotNull UUID userId,
        @NotNull UUID tenantId,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 128) String password,
        @NotNull Role role
) {
}
