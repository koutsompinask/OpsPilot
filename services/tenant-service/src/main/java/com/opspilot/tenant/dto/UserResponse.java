package com.opspilot.tenant.dto;

import com.opspilot.tenant.domain.entity.Role;
import java.util.UUID;

/**
 * Response payload representing a tenant-scoped user profile.
 *
 * <p>{@code userId} is the platform-wide user UUID shared with auth-service.
 */
public record UserResponse(
        UUID userId,
        UUID tenantId,
        String displayName,
        String email,
        Role role
) {
}
