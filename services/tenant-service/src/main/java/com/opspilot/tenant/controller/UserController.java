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

@RestController
@RequestMapping("/users")
public class UserController {

    private final TenantService tenantService;
    private final CurrentUserResolver currentUserResolver;

    public UserController(TenantService tenantService, CurrentUserResolver currentUserResolver) {
        this.tenantService = tenantService;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping
    public List<UserResponse> listUsers(@AuthenticationPrincipal Jwt jwt) {
        CurrentUser currentUser = currentUserResolver.fromJwt(jwt);
        if (!currentUser.isAdmin()) {
            throw new ForbiddenException("Only tenant admins can list users");
        }
        return tenantService.listUsers(currentUser.tenantId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateUserRequest request) {
        CurrentUser currentUser = currentUserResolver.fromJwt(jwt);
        if (!currentUser.isAdmin()) {
            throw new ForbiddenException("Only tenant admins can create users");
        }
        return tenantService.createUser(currentUser, request);
    }
}
