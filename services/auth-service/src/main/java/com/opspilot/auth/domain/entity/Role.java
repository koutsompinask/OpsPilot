package com.opspilot.auth.domain.entity;

/**
 * Enumerates the roles that can be assigned to users within a tenant.
 *
 * <p>The role name is stored as a plain string in the database ({@code EnumType.STRING}) and
 * is embedded as a claim in the JWT access token so that downstream services can perform
 * role-based access control without an additional lookup.</p>
 */
public enum Role {
    /** The first admin user of a tenant, created during the registration flow. */
    TENANT_ADMIN,
    /** A regular member user provisioned within an existing tenant. */
    TENANT_MEMBER
}
