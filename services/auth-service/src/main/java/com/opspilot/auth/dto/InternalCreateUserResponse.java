package com.opspilot.auth.dto;

import com.opspilot.auth.entity.Role;
import java.util.UUID;

public record InternalCreateUserResponse(
        UUID userId,
        UUID tenantId,
        String email,
        Role role
) {
}
