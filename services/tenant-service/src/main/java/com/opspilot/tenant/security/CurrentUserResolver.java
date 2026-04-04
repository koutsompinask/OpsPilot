package com.opspilot.tenant.security;

import com.opspilot.tenant.domain.entity.Role;
import com.opspilot.tenant.exception.UnauthorizedException;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Converts a Spring Security {@link Jwt} into a {@link CurrentUser} value object.
 *
 * <p>This component centralises JWT claim extraction so that controllers and services
 * never need to read raw claims directly. Any parsing failure (missing claim, malformed
 * UUID, unrecognised role value) is surfaced as an {@link UnauthorizedException} rather
 * than a generic 500, ensuring a consistent error response for bad tokens.
 */
@Component
public class CurrentUserResolver {

    /**
     * Extracts caller identity from a validated JWT and returns it as a {@link CurrentUser}.
     *
     * <p>Claim mapping:
     * <ul>
     *   <li>{@code sub} (standard JWT subject) → {@link CurrentUser#userId()}</li>
     *   <li>{@code tenant_id} (custom claim) → {@link CurrentUser#tenantId()}</li>
     *   <li>{@code role} (custom claim, matches {@link Role} enum name) → {@link CurrentUser#role()}</li>
     *   <li>{@code email} (custom claim) → {@link CurrentUser#email()}</li>
     * </ul>
     *
     * @param jwt the validated JWT provided by Spring Security's OAuth2 resource-server filter
     * @return a fully populated {@link CurrentUser} for the authenticated caller
     * @throws UnauthorizedException if any required claim is absent, not a valid UUID, or not a known role
     */
    public CurrentUser fromJwt(Jwt jwt) {
        try {
            // sub claim holds the auth-service user UUID (shared primary key across services)
            UUID userId = UUID.fromString(jwt.getSubject());
            // tenant_id is a custom claim injected by auth-service at token issuance time
            UUID tenantId = UUID.fromString(jwt.getClaimAsString("tenant_id"));
            // role is stored as the enum constant name (e.g. "TENANT_ADMIN")
            Role role = Role.valueOf(jwt.getClaimAsString("role"));
            String email = jwt.getClaimAsString("email");
            return new CurrentUser(userId, tenantId, email, role);
        } catch (Exception ex) {
            // Wrap any parsing exception so callers get a consistent 401 response
            throw new UnauthorizedException("Invalid authentication token");
        }
    }
}
