package com.opspilot.tenant.controller;

import com.opspilot.tenant.dto.CreateUserRequest;
import com.opspilot.tenant.dto.UserResponse;
import com.opspilot.tenant.exception.ForbiddenException;
import com.opspilot.tenant.security.CurrentUser;
import com.opspilot.tenant.security.CurrentUserResolver;
import com.opspilot.tenant.service.TenantService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing user-management operations for tenant-scoped user accounts.
 *
 * <p>All operations are restricted to callers with the {@code TENANT_ADMIN} role and are
 * automatically scoped to the tenant embedded in the caller's JWT. Creating a user triggers
 * bi-directional provisioning: this service calls auth-service to create the auth account,
 * then persists the local profile.
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private final TenantService tenantService;
    private final CurrentUserResolver currentUserResolver;

    public UserController(TenantService tenantService, CurrentUserResolver currentUserResolver) {
        this.tenantService = tenantService;
        this.currentUserResolver = currentUserResolver;
    }

    /**
     * Lists user profiles belonging to the authenticated caller's tenant, with pagination.
     *
     * <p>Defaults to 20 users per page sorted by display name ascending. Callers can override
     * via standard Spring {@code ?page=}, {@code ?size=}, and {@code ?sort=} query params.
     *
     * @param jwt      the JWT injected by Spring Security's OAuth2 resource-server filter
     * @param pageable pagination and sort parameters; defaults to page 0, size 20, name ascending
     * @return a page of user profiles for the caller's tenant
     * @throws ForbiddenException if the caller is not a tenant administrator
     */
    @GetMapping
    public Page<UserResponse> listUsers(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20, sort = "displayName", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        CurrentUser currentUser = currentUserResolver.fromJwt(jwt);
        // Admin-only enforcement: member-level callers must not see peer profiles
        if (!currentUser.isAdmin()) {
            throw new ForbiddenException("Only tenant admins can list users");
        }
        return tenantService.listUsers(currentUser.tenantId(), pageable);
    }

    /**
     * Creates a new user within the authenticated caller's tenant.
     *
     * <p>This operation provisions the user in both auth-service (credentials) and
     * tenant-service (profile). The {@code role} field defaults to {@code TENANT_MEMBER}
     * if not supplied in the request body.
     *
     * @param jwt     the JWT injected by Spring Security's OAuth2 resource-server filter
     * @param request the new user's display name, email, password, and optional role
     * @return the newly created user profile
     * @throws ForbiddenException if the caller is not a tenant administrator
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateUserRequest request) {
        CurrentUser currentUser = currentUserResolver.fromJwt(jwt);
        // Admin-only enforcement: only TENANT_ADMIN may invite new users to the tenant
        if (!currentUser.isAdmin()) {
            throw new ForbiddenException("Only tenant admins can create users");
        }
        return tenantService.createUser(currentUser, request);
    }
}
