package com.opspilot.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for the {@code POST /auth/login} endpoint.
 *
 * @param email    the user's email address; must be a syntactically valid email
 * @param password the user's plaintext password
 */
public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {
}
