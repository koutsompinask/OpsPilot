package com.opspilot.auth.service.integration;

import com.opspilot.auth.domain.entity.Role;
import java.util.UUID;

public record TenantBootstrapRequest(
        UUID tenantId,
        String tenantName,
        UUID adminUserId,
        String adminName,
        String adminEmail,
        Role role
) {
}
