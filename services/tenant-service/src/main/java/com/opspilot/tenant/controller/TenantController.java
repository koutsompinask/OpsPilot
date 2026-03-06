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

@RestController
@RequestMapping("/tenants")
public class TenantController {

    private final TenantService tenantService;
    private final CurrentUserResolver currentUserResolver;

    public TenantController(TenantService tenantService, CurrentUserResolver currentUserResolver) {
        this.tenantService = tenantService;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping("/me")
    public TenantResponse getMyTenant(@AuthenticationPrincipal Jwt jwt) {
        CurrentUser currentUser = currentUserResolver.fromJwt(jwt);
        return tenantService.getTenant(currentUser.tenantId());
    }

    @PutMapping("/me")
    public TenantResponse updateMyTenant(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateTenantRequest request) {
        CurrentUser currentUser = currentUserResolver.fromJwt(jwt);
        if (!currentUser.isAdmin()) {
            throw new ForbiddenException("Only tenant admins can update tenant settings");
        }
        return tenantService.updateTenant(currentUser.tenantId(), request);
    }
}
