package com.opspilot.ticket.config;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Configures JWT decoding for the ticket-service OAuth2 resource server.
 *
 * <p>Tokens are issued by the auth-service using HMAC-SHA256 with a shared secret. The same secret
 * must be present in this service's configuration so that incoming JWTs can be verified without
 * contacting a remote authorisation server.</p>
 */
@Configuration
public class JwtConfig {

    /**
     * Creates a {@link JwtDecoder} that validates HS256-signed tokens using the configured shared secret.
     *
     * @param secret the HMAC-SHA256 signing secret, injected from
     *               {@code spring.security.oauth2.resourceserver.jwt.secret}
     * @return a fully configured {@link NimbusJwtDecoder}
     */
    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.secret}") String secret,
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer}") String issuer) {
        // Reconstruct the SecretKey from the raw UTF-8 bytes of the shared secret
        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        // Validate the issuer claim to reject tokens not issued by auth-service
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }
}
