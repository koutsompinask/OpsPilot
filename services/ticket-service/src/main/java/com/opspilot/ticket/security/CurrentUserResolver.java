package com.opspilot.ticket.security;

import com.opspilot.ticket.domain.entity.Role;
import com.opspilot.ticket.exception.UnauthorizedException;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserResolver {

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
