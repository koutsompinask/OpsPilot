package com.opspilot.auth.repository;

import com.opspilot.auth.domain.entity.RefreshSession;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link RefreshSession} records.
 *
 * <p>Sessions are looked up exclusively by their SHA-256 token hash; the raw token is never
 * stored and therefore cannot be queried directly.</p>
 */
public interface RefreshSessionRepository extends JpaRepository<RefreshSession, UUID> {

    /**
     * Finds a refresh session by the SHA-256 hash of the raw token.
     *
     * @param tokenHash the Base64-encoded SHA-256 hash of the opaque refresh token
     * @return an {@link Optional} containing the session if found, or empty if the hash is
     *         unknown (i.e. the token was never issued or has been purged)
     */
    Optional<RefreshSession> findByTokenHash(String tokenHash);
}
