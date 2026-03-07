package com.opspilot.tenant.dto;

import com.opspilot.tenant.domain.entity.Role;
import java.util.UUID;

public record InternalCreateAuthUserResponse(
        UUID userId,
        UUID tenantId,
        String email,
        Role role
) {
}
