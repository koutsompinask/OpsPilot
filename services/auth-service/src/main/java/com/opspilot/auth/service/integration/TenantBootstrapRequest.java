package com.opspilot.auth.service.integration;

import com.opspilot.auth.domain.entity.Role;
import java.util.UUID;

/**
 * Payload sent to the tenant-service's internal bootstrap endpoint during tenant registration.
 *
 * Carries the new tenant's identity and the admin user details so the tenant-service can
 * create both the tenant profile and the admin's user profile in a single call.
 *
 * @param tenantId    the UUID assigned to the new tenant
 * @param tenantName  the display name for the new tenant
 * @param adminUserId the UUID of the admin user created in auth-service
 * @param adminName   the admin's display name
 * @param adminEmail  the admin's email address
 * @param role        the role to assign to the admin (always {@code TENANT_ADMIN})
 */
public record TenantBootstrapRequest(
        UUID tenantId,
        String tenantName,
        UUID adminUserId,
        String adminName,
        String adminEmail,
        Role role
) {
}
