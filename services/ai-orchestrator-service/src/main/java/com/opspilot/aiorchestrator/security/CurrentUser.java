package com.opspilot.aiorchestrator.security;

import com.opspilot.aiorchestrator.domain.entity.Role;
import java.util.UUID;

public record CurrentUser(UUID userId, UUID tenantId, String email, Role role) {
}
