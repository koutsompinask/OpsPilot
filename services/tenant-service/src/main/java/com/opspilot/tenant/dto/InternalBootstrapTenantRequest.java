package com.opspilot.tenant.dto;

import com.opspilot.tenant.domain.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record InternalBootstrapTenantRequest(
        @NotNull UUID tenantId,
        @NotBlank String tenantName,
        @NotNull UUID adminUserId,
        @NotBlank String adminName,
        @NotBlank @Email String adminEmail,
        @NotNull Role role
) {
}
