package com.opspilot.tenant.controller;

import com.opspilot.tenant.dto.CreateUserRequest;
import com.opspilot.tenant.dto.UserResponse;
import com.opspilot.tenant.exception.ForbiddenException;
import com.opspilot.tenant.security.CurrentUser;
import com.opspilot.tenant.security.CurrentUserResolver;
import com.opspilot.tenant.service.TenantService;
import jakarta.validation.Valid;
import java.util.List;
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
     * Lists all user profiles belonging to the authenticated caller's tenant.
     *
     * @param jwt the JWT injected by Spring Security's OAuth2 resource-server filter
     * @return all user profiles for the caller's tenant
     * @throws ForbiddenException if the caller is not a tenant administrator
     */
    @GetMapping
    public List<UserResponse> listUsers(@AuthenticationPrincipal Jwt jwt) {
        CurrentUser currentUser = currentUserResolver.fromJwt(jwt);
        // Admin-only enforcement: member-level callers must not see peer profiles
        if (!currentUser.isAdmin()) {
            throw new ForbiddenException("Only tenant admins can list users");
        }
        return tenantService.listUsers(currentUser.tenantId());
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
