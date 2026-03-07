package com.opspilot.tenant.dto;

import com.opspilot.tenant.domain.entity.Role;
import java.util.UUID;

public record UserResponse(
        UUID userId,
        UUID tenantId,
        String displayName,
        String email,
        Role role
) {
}
