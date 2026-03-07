package com.opspilot.tenant.dto;

import com.opspilot.tenant.domain.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank String displayName,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 128) String password,
        Role role
) {
}
