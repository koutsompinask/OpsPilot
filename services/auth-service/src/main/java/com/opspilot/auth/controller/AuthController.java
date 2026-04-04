package com.opspilot.auth.controller;

import com.opspilot.auth.dto.LoginRequest;
import com.opspilot.auth.dto.RefreshTokenRequest;
import com.opspilot.auth.dto.RegisterRequest;
import com.opspilot.auth.dto.TokenResponse;
import com.opspilot.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public-facing REST controller that exposes tenant registration, user login, and token
 * refresh endpoints.
 *
 * <p>All three endpoints are unauthenticated (see {@link com.opspilot.auth.config.SecurityConfig})
 * because they are the entry-point for credential issuance. Each successful response returns a
 * {@link TokenResponse} containing a short-lived access token and a long-lived refresh token.</p>
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Registers a new tenant and its first admin user, then issues an initial token pair.
     *
     * <p>Triggers a call to tenant-service to bootstrap the tenant profile before persisting the
     * credential record. Returns {@code 201 Created} on success.</p>
     *
     * @param request validated registration payload containing tenant name, admin details, and
     *                password
     * @return a {@link TokenResponse} containing access and refresh tokens for the new admin user
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    /**
     * Authenticates an existing user and issues a fresh token pair.
     *
     * @param request validated login payload containing email and plaintext password
     * @return a {@link TokenResponse} containing access and refresh tokens
     */
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /**
     * Exchanges a valid refresh token for a new access token and a rotated refresh token.
     *
     * <p>The supplied refresh token is revoked immediately (single-use rotation), preventing
     * replay attacks even if the token is intercepted after use.</p>
     *
     * @param request payload containing the raw refresh token string
     * @return a {@link TokenResponse} with a new access token and a newly issued refresh token
     */
    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request);
    }
}
