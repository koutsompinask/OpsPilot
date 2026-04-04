package com.opspilot.tenant.controller;

import com.opspilot.tenant.dto.TenantResponse;
import com.opspilot.tenant.dto.UpdateTenantRequest;
import com.opspilot.tenant.exception.ForbiddenException;
import com.opspilot.tenant.security.CurrentUser;
import com.opspilot.tenant.security.CurrentUserResolver;
import com.opspilot.tenant.service.TenantService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing tenant-profile operations to authenticated users.
 *
 * <p>All endpoints are scoped to the tenant embedded in the caller's JWT, so a user can only
 * ever read or modify their own tenant. The {@code PUT /tenants/me} endpoint additionally
 * enforces that the caller holds the {@code TENANT_ADMIN} role.
 */
@RestController
@RequestMapping("/tenants")
public class TenantController {

    private final TenantService tenantService;
    private final CurrentUserResolver currentUserResolver;

    public TenantController(TenantService tenantService, CurrentUserResolver currentUserResolver) {
        this.tenantService = tenantService;
        this.currentUserResolver = currentUserResolver;
    }

    /**
     * Returns the tenant profile for the authenticated caller's tenant.
     *
     * @param jwt the JWT injected by Spring Security's OAuth2 resource-server filter
     * @return the caller's tenant profile
     */
    @GetMapping("/me")
    public TenantResponse getMyTenant(@AuthenticationPrincipal Jwt jwt) {
        CurrentUser currentUser = currentUserResolver.fromJwt(jwt);
        return tenantService.getTenant(currentUser.tenantId());
    }

    /**
     * Updates the tenant profile (name and settings) for the authenticated caller's tenant.
     *
     * <p>Only users with the {@code TENANT_ADMIN} role may call this endpoint. Regular members
     * receive a 403 Forbidden response.
     *
     * @param jwt     the JWT injected by Spring Security's OAuth2 resource-server filter
     * @param request the updated tenant name and optional settings JSON
     * @return the updated tenant profile
     * @throws ForbiddenException if the caller is not a tenant administrator
     */
    @PutMapping("/me")
    public TenantResponse updateMyTenant(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateTenantRequest request) {
        CurrentUser currentUser = currentUserResolver.fromJwt(jwt);
        // Admin-only enforcement: only TENANT_ADMIN may mutate tenant settings
        if (!currentUser.isAdmin()) {
            throw new ForbiddenException("Only tenant admins can update tenant settings");
        }
        return tenantService.updateTenant(currentUser.tenantId(), request);
    }
}
