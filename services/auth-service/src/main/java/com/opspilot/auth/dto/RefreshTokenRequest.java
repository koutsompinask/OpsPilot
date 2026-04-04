package com.opspilot.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for the {@code POST /auth/refresh} endpoint.
 *
 * @param refreshToken the opaque refresh token previously issued by the auth-service; must not
 *                     be blank
 */
public record RefreshTokenRequest(@NotBlank String refreshToken) {
}
