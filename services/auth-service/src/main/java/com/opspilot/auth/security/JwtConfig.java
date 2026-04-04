package com.opspilot.auth.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Infrastructure configuration for JWT encoding and the shared application clock.
 *
 * <p>Wires the HMAC-SHA-256 symmetric {@link JwtEncoder} used by {@link JwtService} to sign
 * access tokens. The secret is loaded from the {@code auth.jwt.secret} property so it can be
 * rotated via environment variables without code changes. A single UTC {@link Clock} bean is
 * registered here so that token expiry calculations are testable by substituting a fixed clock.</p>
 */
@Configuration
public class JwtConfig {

    /**
     * Creates a {@link NimbusJwtEncoder} backed by an HMAC-SHA-256 symmetric key derived from
     * the configured secret string.
     *
     * @param secret the raw secret value from {@code auth.jwt.secret}; must be at least 32
     *               characters to provide 256 bits of key material
     * @return a {@link JwtEncoder} that signs tokens with HS256
     */
    @Bean
    JwtEncoder jwtEncoder(@Value("${auth.jwt.secret}") String secret) {
        // Derive the HMAC-SHA-256 key directly from the UTF-8 bytes of the configured secret
        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    /**
     * Provides the application-wide {@link Clock} used for token timestamp calculations.
     *
     * <p>Always UTC so that issued-at and expiry instants are timezone-independent.</p>
     *
     * @return a UTC system clock
     */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
