package com.opspilot.auth.security;

import com.opspilot.auth.domain.entity.AuthUser;
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

/**
 * Issues signed JWT access tokens for authenticated users.
 *
 * <p>Tokens are signed with HMAC-SHA-256 using the secret configured in {@code auth.jwt.secret}.
 * The TTL is controlled by {@code auth.jwt.access-token-ttl} (typically 15 minutes). The
 * complementary long-lived refresh token is managed separately by
 * {@link com.opspilot.auth.service.RefreshTokenService}.</p>
 */
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

    /**
     * Builds and signs a JWT access token for the given user.
     *
     * <p>Access tokens are short-lived (15 min); refresh tokens are long-lived (14 days) to
     * balance security and UX. The token encodes the minimum set of claims needed by downstream
     * services to resolve the caller's identity without a database round-trip:</p>
     * <ul>
     *   <li>{@code sub} — the user's UUID, used as the principal identifier</li>
     *   <li>{@code tenant_id} — UUID of the owning tenant, used for data isolation</li>
     *   <li>{@code role} — the user's role (e.g. {@code TENANT_ADMIN}), used for authorisation</li>
     *   <li>{@code email} — the user's normalised email, included for display purposes</li>
     * </ul>
     *
     * @param user the authenticated user whose identity is encoded in the token
     * @return a compact, URL-safe HS256-signed JWT string
     */
    public String issueAccessToken(AuthUser user) {
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plus(accessTokenTtl);

        // Build the claims set; all downstream services rely on tenant_id and role for
        // multi-tenant isolation and access control checks
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(expiresAt)
                // sub = user UUID — principal identifier used across all services
                .subject(user.getId().toString())
                // tenant_id — propagated to every downstream service for tenant scoping
                .claim("tenant_id", user.getTenantId().toString())
                // role — used by resource servers for endpoint-level authorisation
                .claim("role", user.getRole().name())
                // email — convenience claim; avoids a separate profile lookup in UIs
                .claim("email", user.getEmail())
                .build();

        // HS256 is used rather than RS256 to avoid managing a key-pair; acceptable here
        // because auth-service is the sole issuer and all resource servers share the secret
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /**
     * Returns the access token lifetime in seconds, suitable for inclusion in a token
     * response ({@code expires_in} field).
     *
     * @return the configured access token TTL in seconds
     */
    public long accessTokenExpiresInSeconds() {
        return accessTokenTtl.getSeconds();
    }
}
