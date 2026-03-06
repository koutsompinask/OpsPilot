package com.opspilot.auth.client;

import com.opspilot.auth.entity.Role;
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
