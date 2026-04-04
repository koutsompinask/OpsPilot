package com.opspilot.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for the {@code POST /auth/register} endpoint.
 *
 * <p>Carries all the information needed to bootstrap a new tenant and create its first admin
 * user in a single call. The email is normalised (lowercased, trimmed) in the service layer
 * before use.</p>
 *
 * @param tenantName  display name of the new tenant organisation
 * @param adminName   display name of the initial admin user
 * @param email       valid email address for the admin user; must be unique across all tenants
 * @param password    plaintext password; must be 8–128 characters
 */
public record RegisterRequest(
        @NotBlank String tenantName,
        @NotBlank String adminName,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 128) String password
) {
}
