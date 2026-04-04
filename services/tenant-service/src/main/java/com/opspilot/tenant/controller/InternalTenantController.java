package com.opspilot.tenant.controller;

import com.opspilot.tenant.dto.InternalBootstrapTenantRequest;
import com.opspilot.tenant.exception.ForbiddenException;
import com.opspilot.tenant.service.TenantService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal HTTP endpoint that handles the tenant bootstrap flow initiated by auth-service.
 *
 * <p>This controller is intentionally excluded from JWT authentication (see {@link SecurityConfig})
 * because auth-service calls it during registration — before the new tenant's users have
 * any JWTs. Instead, the endpoint is guarded by a shared service token checked on every
 * request. The path prefix {@code /internal/**} must never be exposed through the API gateway.
 */
@RestController
@RequestMapping("/internal/tenants")
public class InternalTenantController {

    private final TenantService tenantService;
    private final String serviceToken;

    public InternalTenantController(TenantService tenantService, @Value("${tenant.service-token}") String serviceToken) {
        this.tenantService = tenantService;
        this.serviceToken = serviceToken;
    }

    /**
     * Creates the initial tenant record and admin user profile as part of the registration flow.
     *
     * <p>This endpoint is called by auth-service immediately after it has persisted the admin
     * user's auth account. The reason auth-service initiates this call — rather than the user
     * calling tenant-service directly — is that a JWT for the new user does not yet exist at
     * registration time. auth-service therefore acts as a trusted orchestrator for the bootstrap.
     *
     * <p>The shared {@code X-Service-Token} header is validated before any processing occurs.
     * If the token is absent or incorrect, the request is rejected with 403 Forbidden.
     *
     * @param providedToken the service token from the {@code X-Service-Token} request header
     * @param request       tenant and admin-user details sent by auth-service
     * @throws ForbiddenException if the provided service token does not match the configured value
     */
    @PostMapping("/bootstrap")
    @ResponseStatus(HttpStatus.CREATED)
    public void bootstrapTenant(
            @RequestHeader(name = "X-Service-Token", required = false) String providedToken,
            @Valid @RequestBody InternalBootstrapTenantRequest request
    ) {
        // Validate the shared service token before processing — this is the only auth mechanism for /internal endpoints
        if (!serviceToken.equals(providedToken)) {
            throw new ForbiddenException("Invalid service token");
        }
        tenantService.bootstrapTenant(request);
    }
}
