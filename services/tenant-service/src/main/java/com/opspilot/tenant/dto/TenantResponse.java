package com.opspilot.tenant.dto;

import java.util.UUID;

/**
 * Response payload representing a tenant profile returned from the tenant API.
 *
 * <p>{@code settingsJson} may be {@code null} when no tenant-level configuration has been saved.
 */
public record TenantResponse(
        UUID id,
        String name,
        String settingsJson
) {
}
