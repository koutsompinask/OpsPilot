package com.opspilot.ticket.security;

import com.opspilot.ticket.domain.entity.Role;
import com.opspilot.ticket.exception.UnauthorizedException;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Extracts and validates the authenticated user's identity from a verified JWT.
 *
 * <p>The JWT is expected to carry the following custom claims, injected by the auth-service at
 * token issuance time: {@code tenant_id} (UUID), {@code email} (string), and {@code role}
 * (one of the {@link Role} enum values). The standard {@code sub} claim holds the user's UUID.</p>
 */
@Component
public class CurrentUserResolver {

    /**
     * Converts a Spring Security {@link Jwt} into a {@link CurrentUser} value object.
     *
     * <p>Any missing or malformed claim causes an {@link UnauthorizedException} so that the
     * caller receives a 401 rather than an unhandled exception. Validation of the JWT signature
     * and expiry is handled upstream by Spring Security's OAuth2 resource server filter.</p>
     *
     * @param jwt the verified JWT principal injected by Spring Security
     * @return a fully-populated {@link CurrentUser} derived from the token's claims
     * @throws UnauthorizedException if any required claim is absent or cannot be parsed
     */
    public CurrentUser fromJwt(Jwt jwt) {
        try {
            UUID userId = UUID.fromString(jwt.getSubject());
            UUID tenantId = UUID.fromString(jwt.getClaimAsString("tenant_id"));
            String email = jwt.getClaimAsString("email");
            Role role = Role.valueOf(jwt.getClaimAsString("role"));
            return new CurrentUser(userId, tenantId, email, role);
        } catch (Exception ex) {
            throw new UnauthorizedException("Invalid authentication token");
        }
    }
}
