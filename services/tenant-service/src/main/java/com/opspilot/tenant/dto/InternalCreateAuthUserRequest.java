package com.opspilot.tenant.dto;

import com.opspilot.tenant.entity.Role;
import java.util.UUID;

public record InternalCreateAuthUserRequest(
        UUID userId,
        UUID tenantId,
        String email,
        String password,
        Role role
) {
}
