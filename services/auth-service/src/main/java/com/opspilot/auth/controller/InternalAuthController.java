package com.opspilot.auth.controller;

import com.opspilot.auth.dto.InternalCreateUserRequest;
import com.opspilot.auth.dto.InternalCreateUserResponse;
import com.opspilot.auth.exception.ForbiddenException;
import com.opspilot.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal REST controller that allows trusted backend services to create auth users without
 * going through the public registration flow.
 *
 * <p>This endpoint is called exclusively by the tenant-service when provisioning a new member
 * user inside an existing tenant. Callers must supply the shared {@code X-Service-Token} header;
 * any request with a missing or incorrect token is rejected with {@code 403 Forbidden} before
 * any business logic executes. The endpoint is mounted under {@code /internal/**} which Spring
 * Security permits without JWT authentication (see
 * {@link com.opspilot.auth.config.SecurityConfig}), relying solely on the service token for
 * access control.</p>
 */
@RestController
@RequestMapping("/internal/auth")
public class InternalAuthController {

    private final AuthService authService;
    private final String serviceToken;

    public InternalAuthController(AuthService authService, @Value("${auth.service-token}") String serviceToken) {
        this.authService = authService;
        this.serviceToken = serviceToken;
    }

    /**
     * Creates a new auth credential record for a user that already exists in tenant-service.
     *
     * <p>The caller must pass the correct shared service token in the {@code X-Service-Token}
     * header. This is an internal service-to-service authentication pattern: rather than mutual
     * TLS or OAuth client credentials, a pre-shared bearer-style token is used for simplicity
     * within the trusted internal network.</p>
     *
     * @param providedToken the value of the {@code X-Service-Token} header; may be {@code null}
     *                      if the header is absent
     * @param request       validated payload specifying the user ID, tenant ID, email, password,
     *                      and role for the new user
     * @return an {@link InternalCreateUserResponse} containing the newly created user's
     *         identifiers and assigned role
     * @throws ForbiddenException if the provided service token does not match the configured value
     */
    @PostMapping("/users")
    public InternalCreateUserResponse createUser(
            @RequestHeader(name = "X-Service-Token", required = false) String providedToken,
            @Valid @RequestBody InternalCreateUserRequest request
    ) {
        // Guard: reject the call immediately if the service token is absent or incorrect
        if (!serviceToken.equals(providedToken)) {
            throw new ForbiddenException("Invalid service token");
        }
        return authService.createInternalUser(request);
    }
}
