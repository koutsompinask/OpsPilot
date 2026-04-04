package com.opspilot.auth.dto;

/**
 * Response payload returned to the client after a successful authentication or token refresh.
 *
 * <p>The shape follows the OAuth 2.0 token response convention: {@code access_token},
 * {@code refresh_token}, {@code expires_in} (seconds until the access token expires), and
 * {@code token_type} (always {@code "Bearer"}).</p>
 *
 * @param accessToken  the signed JWT access token; short-lived (typically 15 minutes)
 * @param refreshToken the opaque refresh token; long-lived (typically 14 days) and single-use
 * @param expiresIn    seconds until the access token expires, for client-side refresh scheduling
 * @param tokenType    the token scheme — always {@code "Bearer"}
 */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        String tokenType
) {
}
