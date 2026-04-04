package com.opspilot.apigateway.config;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

/**
 * Configures JWT validation for the API Gateway's OAuth2 resource server.
 *
 * OpsPilot uses HS256 (HMAC-SHA256) symmetric signing rather than an asymmetric
 * algorithm such as RS256. This means all services share the same secret to both
 * sign (auth-service) and verify (gateway + downstream services) tokens. The
 * trade-off is simpler key management in a private, controlled deployment at the
 * cost of not being able to rotate keys per-service independently.
 */
@Configuration
public class JwtConfig {

    /**
     * Builds a reactive JWT decoder that validates tokens signed with the shared
     * HS256 secret.
     *
     * <p>The secret is injected from the {@code gateway.jwt.secret} property so it
     * can be supplied via environment variable or a secrets manager at runtime
     * without being baked into the container image.
     *
     * @param secret the raw HMAC secret shared with auth-service and all downstream
     *               services that independently verify JWTs
     * @return a {@link ReactiveJwtDecoder} wired into Spring Security's OAuth2
     *         resource server filter chain
     */
    @Bean
    public ReactiveJwtDecoder jwtDecoder(
            @Value("${gateway.jwt.secret}") String secret,
            @Value("${gateway.jwt.issuer}") String issuer) {
        // Derive the HMAC key from the raw UTF-8 bytes of the shared secret string
        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withSecretKey(key)
                // Explicitly pin the algorithm to HS256; reject tokens signed with anything else
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        // Validate the issuer claim to ensure tokens were issued by auth-service,
        // preventing token substitution from other sources using the same shared secret
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }
}
