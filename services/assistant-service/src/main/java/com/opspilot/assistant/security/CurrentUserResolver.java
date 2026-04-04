package com.opspilot.assistant.security;

import com.opspilot.assistant.domain.entity.Role;
import com.opspilot.assistant.exception.UnauthorizedException;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Extracts and validates the authenticated user's identity from a Spring Security JWT.
 *
 * <p>The JWT is issued by the auth-service and carries custom claims: {@code tenant_id},
 * {@code role}, and {@code email} alongside the standard {@code sub} (user ID). Any
 * missing or malformed claim results in an {@link com.opspilot.assistant.exception.UnauthorizedException}
 * so that the caller never receives a partially-constructed {@link CurrentUser}.</p>
 */
@Component
public class CurrentUserResolver {

    /**
     * Builds a {@link CurrentUser} from the claims in the provided JWT.
     *
     * @param jwt the validated JWT supplied by Spring Security's OAuth2 resource server filter
     * @return a fully populated {@link CurrentUser} ready for use in the service layer
     * @throws com.opspilot.assistant.exception.UnauthorizedException if any required claim is absent or malformed
     */
    public CurrentUser fromJwt(Jwt jwt) {
        try {
            UUID userId = UUID.fromString(jwt.getSubject());
            // Custom claim injected by auth-service to scope all data access to the tenant
            UUID tenantId = UUID.fromString(jwt.getClaimAsString("tenant_id"));
            Role role = Role.valueOf(jwt.getClaimAsString("role"));
            String email = jwt.getClaimAsString("email");
            return new CurrentUser(userId, tenantId, email, role);
        } catch (Exception ex) {
            throw new UnauthorizedException("Invalid authentication token");
        }
    }
}
