package com.opspilot.auth.service;

import com.opspilot.auth.entity.AuthUser;
import com.opspilot.auth.entity.RefreshSession;
import com.opspilot.auth.exception.UnauthorizedException;
import com.opspilot.auth.repository.RefreshSessionRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenService {

    private final RefreshSessionRepository refreshSessionRepository;
    private final Duration refreshTokenTtl;
    private final Clock clock;

    public RefreshTokenService(
            RefreshSessionRepository refreshSessionRepository,
            @Value("${auth.jwt.refresh-token-ttl}") Duration refreshTokenTtl,
            Clock clock
    ) {
        this.refreshSessionRepository = refreshSessionRepository;
        this.refreshTokenTtl = refreshTokenTtl;
        this.clock = clock;
    }

    public IssuedRefreshToken issue(AuthUser user, String metadata) {
        String rawToken = generateRawToken();
        String tokenHash = hash(rawToken);

        RefreshSession session = new RefreshSession();
        session.setId(UUID.randomUUID());
        session.setUser(user);
        session.setTokenHash(tokenHash);
        session.setExpiresAt(Instant.now(clock).plus(refreshTokenTtl));
        session.setMetadata(metadata);
        refreshSessionRepository.save(session);

        return new IssuedRefreshToken(rawToken, session);
    }

    public RefreshSession validate(String rawToken) {
        String tokenHash = hash(rawToken);
        RefreshSession session = refreshSessionRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        Instant now = Instant.now(clock);
        if (session.getRevokedAt() != null || session.getExpiresAt().isBefore(now)) {
            throw new UnauthorizedException("Refresh token is expired or revoked");
        }
        if (!session.getUser().isActive()) {
            throw new UnauthorizedException("User is not active");
        }
        return session;
    }

    public void revoke(RefreshSession session) {
        session.setRevokedAt(Instant.now(clock));
        refreshSessionRepository.save(session);
    }

    private String generateRawToken() {
        return UUID.randomUUID().toString() + UUID.randomUUID();
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", ex);
        }
    }

    public record IssuedRefreshToken(String rawToken, RefreshSession session) {
    }
}
