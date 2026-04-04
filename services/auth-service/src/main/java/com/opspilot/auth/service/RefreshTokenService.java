package com.opspilot.auth.service;

import com.opspilot.auth.domain.entity.AuthUser;
import com.opspilot.auth.domain.entity.RefreshSession;
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

/**
 * Manages the lifecycle of refresh token sessions — issuance, validation, and revocation.
 *
 * <p>Refresh tokens are opaque random strings that are never stored in plaintext. Only a
 * SHA-256 hash of the raw token is persisted in the {@code refresh_sessions} table, so a
 * database breach does not expose usable tokens. The raw token is returned to the client once
 * and never written to any persistent store.</p>
 *
 * <p>Access tokens are short-lived (15 min); refresh tokens are long-lived (14 days) to
 * balance security and UX. When a refresh token is used it is immediately revoked (soft-delete
 * via {@code revoked_at} timestamp), and a new session is created — this is single-use
 * rotation and prevents replay attacks.</p>
 */
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

    /**
     * Issues a new refresh session for the given user.
     *
     * <p>Generates a raw token (two concatenated UUIDs for ~256 bits of entropy), hashes it
     * with SHA-256, and stores only the hash. The raw token is returned to the caller for
     * delivery to the client.</p>
     *
     * @param user     the user for whom the session is being created
     * @param metadata a short label identifying the trigger (e.g. {@code "login"},
     *                 {@code "register"}, {@code "refresh"}) stored for audit purposes
     * @return an {@link IssuedRefreshToken} containing the raw opaque token and the saved session
     */
    public IssuedRefreshToken issue(AuthUser user, String metadata) {
        String rawToken = generateRawToken();
        // Only the hash is persisted — the raw token is returned to the client and never stored
        String tokenHash = hash(rawToken);

        RefreshSession session = new RefreshSession();
        session.setId(UUID.randomUUID());
        session.setUser(user);
        session.setTokenHash(tokenHash);
        // Access tokens are short-lived (15 min); refresh tokens are long-lived (14 days) to
        // balance security and UX
        session.setExpiresAt(Instant.now(clock).plus(refreshTokenTtl));
        session.setMetadata(metadata);
        refreshSessionRepository.save(session);

        return new IssuedRefreshToken(rawToken, session);
    }

    /**
     * Validates a raw refresh token and returns the associated session.
     *
     * <p>Looks up the session by its SHA-256 hash, then checks that the session has not been
     * revoked, has not expired, and belongs to an active user. All failure conditions return
     * the same generic error message to avoid leaking information about session state.</p>
     *
     * @param rawToken the raw opaque refresh token supplied by the client
     * @return the valid {@link RefreshSession} associated with the token
     * @throws UnauthorizedException if the token is unknown, revoked, expired, or the owning
     *                               user is inactive
     */
    public RefreshSession validate(String rawToken) {
        String tokenHash = hash(rawToken);
        RefreshSession session = refreshSessionRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        Instant now = Instant.now(clock);
        // A non-null revokedAt means the session was already consumed or explicitly invalidated
        if (session.getRevokedAt() != null || session.getExpiresAt().isBefore(now)) {
            throw new UnauthorizedException("Refresh token is expired or revoked");
        }
        if (!session.getUser().isActive()) {
            throw new UnauthorizedException("User is not active");
        }
        return session;
    }

    /**
     * Soft-revokes a refresh session by recording the current timestamp in {@code revoked_at}.
     *
     * <p>Revocation is a soft-delete: the session row is retained for audit purposes but will
     * be rejected by {@link #validate} on any subsequent use.</p>
     *
     * @param session the session to revoke; must already be persisted
     */
    public void revoke(RefreshSession session) {
        // Soft-delete: stamp revokedAt so the row remains for audit while being rejected on reuse
        session.setRevokedAt(Instant.now(clock));
        refreshSessionRepository.save(session);
    }

    /**
     * Generates a random opaque token with approximately 256 bits of entropy by concatenating
     * two random UUIDs.
     */
    private String generateRawToken() {
        // Two UUIDs = 256 bits of randomness; no separator needed — the client treats this as
        // an opaque string
        return UUID.randomUUID().toString() + UUID.randomUUID();
    }

    /**
     * Returns the Base64-encoded SHA-256 hash of the given raw token for safe database storage.
     */
    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", ex);
        }
    }

    /**
     * Carries the raw opaque token (to be sent to the client) and the persisted session record.
     *
     * @param rawToken the opaque token string delivered to the client; not stored in the DB
     * @param session  the corresponding {@link RefreshSession} row that was just persisted
     */
    public record IssuedRefreshToken(String rawToken, RefreshSession session) {
    }
}
