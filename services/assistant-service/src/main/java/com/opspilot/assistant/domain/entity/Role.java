package com.opspilot.assistant.domain.entity;

/**
 * Role of a user within a tenant, as extracted from the JWT {@code role} claim.
 *
 * Used in the assistant-service to enforce admin-only operations such as document deletion
 * and embedding profile management.
 */
public enum Role {
    TENANT_ADMIN,
    TENANT_MEMBER
}
