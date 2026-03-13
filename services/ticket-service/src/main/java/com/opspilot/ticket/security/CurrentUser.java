package com.opspilot.ticket.security;

import com.opspilot.ticket.domain.entity.Role;
import java.util.UUID;

public record CurrentUser(UUID userId, UUID tenantId, String email, Role role) {

    public boolean isAdmin() {
        return role == Role.TENANT_ADMIN;
    }
}
