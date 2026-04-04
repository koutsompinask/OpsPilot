package com.opspilot.tenant.dto;

import com.opspilot.tenant.domain.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a new user within the calling admin's tenant.
 *
 * <p>The {@code role} field is optional; {@link com.opspilot.tenant.service.TenantService}
 * defaults to {@code TENANT_MEMBER} when it is absent. The password is forwarded to
 * auth-service for hashing and is never stored by tenant-service.
 */
public record CreateUserRequest(
        @NotBlank String displayName,
        @NotBlank @Email String email,
        /** Minimum 8 characters, maximum 128; forwarded to auth-service for hashing. */
        @NotBlank @Size(min = 8, max = 128) String password,
        Role role
) {
}
