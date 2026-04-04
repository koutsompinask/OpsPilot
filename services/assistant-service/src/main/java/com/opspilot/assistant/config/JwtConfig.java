package com.opspilot.assistant.config;

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
 * <p>OpsPilot uses HMAC-SHA256 symmetric JWTs issued by the auth-service. All services
 * share the same secret so they can independently verify tokens without a remote JWKS
 * endpoint.</p>
 */
@Configuration
public class JwtConfig {

    /**
     * Builds a {@link JwtDecoder} that validates HS256-signed tokens with the configured secret.
     *
     * @param secret the shared HMAC secret, injected from {@code spring.security.oauth2.resourceserver.jwt.secret}
     * @return a {@link NimbusJwtDecoder} configured for HS256 verification
     */
    @Bean
    public JwtDecoder jwtDecoder(@Value("${spring.security.oauth2.resourceserver.jwt.secret}") String secret) {
        // Derive the HMAC key from the raw secret bytes using UTF-8 encoding
        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}
