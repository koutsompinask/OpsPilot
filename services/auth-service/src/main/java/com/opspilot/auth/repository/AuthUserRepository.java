package com.opspilot.auth.repository;

import com.opspilot.auth.domain.entity.AuthUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link AuthUser} credential records.
 *
 * <p>Provides email-based lookup and existence checks in addition to the standard
 * {@link JpaRepository} CRUD operations. All email values passed to these methods must already
 * be normalised (lowercased, trimmed) by the calling service layer.</p>
 */
public interface AuthUserRepository extends JpaRepository<AuthUser, UUID> {

    /**
     * Finds an auth user by their normalised email address.
     *
     * @param email the normalised (lowercase, trimmed) email to look up
     * @return an {@link Optional} containing the user if found, or empty if no match exists
     */
    Optional<AuthUser> findByEmail(String email);

    /**
     * Checks whether an auth user with the given normalised email already exists.
     *
     * <p>Preferred over {@link #findByEmail} for conflict checks because it avoids loading the
     * full entity when only existence is needed.</p>
     *
     * @param email the normalised (lowercase, trimmed) email to check
     * @return {@code true} if a record with this email exists, {@code false} otherwise
     */
    boolean existsByEmail(String email);
}
