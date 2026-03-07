package com.opspilot.knowledgebase.security;

import com.opspilot.knowledgebase.domain.entity.Role;
import java.util.UUID;

public record CurrentUser(UUID userId, UUID tenantId, String email, Role role) {

    public boolean isAdmin() {
        return role == Role.TENANT_ADMIN;
    }
}
