package com.opspilot.tenant.dto;

import com.opspilot.tenant.domain.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request payload sent by auth-service to bootstrap a newly registered tenant.
 *
 * <p>All IDs are assigned by auth-service before this call so that the same UUIDs are
 * consistent across both services from the moment the tenant is created.
 */
public record InternalBootstrapTenantRequest(
        @NotNull UUID tenantId,
        @NotBlank String tenantName,
        /** UUID of the admin user already persisted in auth-service. */
        @NotNull UUID adminUserId,
        @NotBlank String adminName,
        @NotBlank @Email String adminEmail,
        @NotNull Role role
) {
}
