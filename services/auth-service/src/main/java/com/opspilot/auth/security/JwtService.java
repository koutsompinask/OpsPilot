package com.opspilot.auth.security;

import com.opspilot.auth.entity.AuthUser;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final Clock clock;
    private final String issuer;
    private final Duration accessTokenTtl;

    public JwtService(
            JwtEncoder jwtEncoder,
            Clock clock,
            @Value("${auth.jwt.issuer}") String issuer,
            @Value("${auth.jwt.access-token-ttl}") Duration accessTokenTtl
    ) {
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
        this.issuer = issuer;
        this.accessTokenTtl = accessTokenTtl;
    }

    public String issueAccessToken(AuthUser user) {
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plus(accessTokenTtl);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getId().toString())
                .claim("tenant_id", user.getTenantId().toString())
                .claim("role", user.getRole().name())
                .claim("email", user.getEmail())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long accessTokenExpiresInSeconds() {
        return accessTokenTtl.getSeconds();
    }
}
