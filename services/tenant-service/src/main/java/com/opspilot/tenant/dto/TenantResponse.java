package com.opspilot.tenant.dto;

import java.util.UUID;

public record TenantResponse(
        UUID id,
        String name,
        String settingsJson
) {
}
