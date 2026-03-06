package com.opspilot.auth.dto;

import com.opspilot.auth.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record InternalCreateUserRequest(
        @NotNull UUID userId,
        @NotNull UUID tenantId,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 128) String password,
        @NotNull Role role
) {
}
