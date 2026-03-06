package com.opspilot.tenant.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateTenantRequest(@NotBlank String name, String settingsJson) {
}
