package com.opspilot.tenant.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for updating the mutable fields of a tenant profile.
 *
 * <p>{@code settingsJson} is optional; passing {@code null} clears any previously saved settings.
 */
public record UpdateTenantRequest(@NotBlank String name, String settingsJson) {
}
