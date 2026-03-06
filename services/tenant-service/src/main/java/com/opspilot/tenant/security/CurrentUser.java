package com.opspilot.tenant.security;

import com.opspilot.tenant.entity.Role;
import java.util.UUID;

public record CurrentUser(UUID userId, UUID tenantId, String email, Role role) {

    public boolean isAdmin() {
        return role == Role.TENANT_ADMIN;
    }
}
