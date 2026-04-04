package com.opspilot.tenant.dto;

import com.opspilot.tenant.domain.entity.Role;
import java.util.UUID;

/**
 * Response returned by auth-service after successfully creating a new credential account.
 *
 * <p>Echoes back the key identity fields so tenant-service can verify the IDs are consistent.
 * This response is currently used for logging and verification; the caller's transaction
 * does not depend on its content.
 */
public record InternalCreateAuthUserResponse(
        UUID userId,
        UUID tenantId,
        String email,
        Role role
) {
}
