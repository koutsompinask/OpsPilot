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

@RestController
@RequestMapping("/internal/tenants")
public class InternalTenantController {

    private final TenantService tenantService;
    private final String serviceToken;

    public InternalTenantController(TenantService tenantService, @Value("${tenant.service-token}") String serviceToken) {
        this.tenantService = tenantService;
        this.serviceToken = serviceToken;
    }

    @PostMapping("/bootstrap")
    @ResponseStatus(HttpStatus.CREATED)
    public void bootstrapTenant(
            @RequestHeader(name = "X-Service-Token", required = false) String providedToken,
            @Valid @RequestBody InternalBootstrapTenantRequest request
    ) {
        if (!serviceToken.equals(providedToken)) {
            throw new ForbiddenException("Invalid service token");
        }
        tenantService.bootstrapTenant(request);
    }
}
