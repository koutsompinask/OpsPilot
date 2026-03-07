package com.opspilot.tenant.security;

import com.opspilot.tenant.domain.entity.Role;
import com.opspilot.tenant.exception.UnauthorizedException;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserResolver {

    public CurrentUser fromJwt(Jwt jwt) {
        try {
            UUID userId = UUID.fromString(jwt.getSubject());
            UUID tenantId = UUID.fromString(jwt.getClaimAsString("tenant_id"));
            Role role = Role.valueOf(jwt.getClaimAsString("role"));
            String email = jwt.getClaimAsString("email");
            return new CurrentUser(userId, tenantId, email, role);
        } catch (Exception ex) {
            throw new UnauthorizedException("Invalid authentication token");
        }
    }
}
