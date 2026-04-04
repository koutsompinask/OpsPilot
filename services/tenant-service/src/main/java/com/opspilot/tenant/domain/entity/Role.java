package com.opspilot.tenant.domain.entity;

/**
 * Role assigned to a user within a tenant.
 *
 * <p>Role values are stored as strings in the database and embedded as the {@code role} claim
 * in JWTs issued by auth-service, so renaming a constant is a breaking change across the platform.
 */
public enum Role {
    /** Full administrative access within the tenant; can manage users and update tenant settings. */
    TENANT_ADMIN,
    /** Standard user within the tenant; read-only access to shared resources. */
    TENANT_MEMBER
}
