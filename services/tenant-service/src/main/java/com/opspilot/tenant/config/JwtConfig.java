package com.opspilot.tenant.config;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Configures the JWT decoder used by Spring Security's OAuth2 resource-server support.
 *
 * <p>Tokens are signed with a shared HMAC-SHA256 secret that is also known to auth-service.
 * Using a symmetric key means all services in the platform can verify tokens without an
 * additional network call to a JWKS endpoint.
 */
@Configuration
public class JwtConfig {

    /**
     * Builds a {@link JwtDecoder} that validates HS256-signed JWTs using the configured shared secret.
     *
     * @param secret the HMAC-SHA256 signing secret, injected from {@code spring.security.oauth2.resourceserver.jwt.secret}
     * @return a configured {@link NimbusJwtDecoder} ready for use by Spring Security
     */
    @Bean
    public JwtDecoder jwtDecoder(@Value("${spring.security.oauth2.resourceserver.jwt.secret}") String secret) {
        // Derive a SecretKey from the raw UTF-8 bytes of the shared secret; must match the algorithm used by auth-service
        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}
