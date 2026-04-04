package com.opspilot.auth.service;

import com.opspilot.auth.domain.entity.AuthUser;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Handles password hashing and verification using an explicit salt plus BCrypt.
 *
 * <p>BCrypt already incorporates an internal salt, but an additional application-managed salt
 * is prepended to the raw password before hashing. This extra salt is stored in a separate
 * {@code password_salt} column and means that the BCrypt hash cannot be cracked even if only
 * the {@code password_hash} column is exfiltrated without the salt column.</p>
 *
 * <p>Hashing strategy summary:</p>
 * <ol>
 *   <li>Generate 24 random bytes via {@link SecureRandom} and Base64url-encode them into a
 *       32-character salt string.</li>
 *   <li>Concatenate {@code salt + rawPassword} and feed the result into BCrypt (via Spring's
 *       {@link org.springframework.security.crypto.password.PasswordEncoder}).</li>
 *   <li>Store the salt and BCrypt hash separately in the {@code auth_users} table.</li>
 * </ol>
 */
@Service
public class PasswordHashService {

    // 24 raw bytes → 32 Base64url characters; provides 192 bits of salt entropy
    private static final int SALT_BYTES = 24;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;

    public PasswordHashService(PasswordEncoder passwordEncoder, SecureRandom secureRandom) {
        this.passwordEncoder = passwordEncoder;
        this.secureRandom = secureRandom;
    }

    /**
     * Hashes a raw password with a freshly generated random salt.
     *
     * <p>The salt is encoded as a Base64url string (without padding) and prepended to the
     * plaintext password before it is passed to BCrypt. Both the salt and the resulting BCrypt
     * hash are returned so they can be stored separately.</p>
     *
     * @param rawPassword the user-supplied plaintext password
     * @return a {@link PasswordHash} record containing the generated salt and the BCrypt hash
     */
    public PasswordHash hash(String rawPassword) {
        // Generate a cryptographically random salt for this credential
        byte[] saltBytes = new byte[SALT_BYTES];
        secureRandom.nextBytes(saltBytes);
        // Encode as URL-safe Base64 without padding for safe column storage
        String salt = Base64.getUrlEncoder().withoutPadding().encodeToString(saltBytes);
        // Prepend the application salt before BCrypt hashing to add a second factor of defence
        String passwordHash = passwordEncoder.encode(salt + rawPassword);
        return new PasswordHash(salt, passwordHash);
    }

    /**
     * Verifies a plaintext password against the stored salt and hash for a given user.
     *
     * <p>Reconstructs the salted input ({@code storedSalt + rawPassword}) and delegates timing-
     * safe comparison to BCrypt via {@link org.springframework.security.crypto.password.PasswordEncoder#matches}.</p>
     *
     * @param rawPassword the user-supplied plaintext password to verify
     * @param user        the {@link AuthUser} whose stored salt and hash are used for comparison
     * @return {@code true} if the password matches, {@code false} otherwise
     */
    public boolean matches(String rawPassword, AuthUser user) {
        // Re-prepend the stored salt before the BCrypt comparison
        return passwordEncoder.matches(user.getPasswordSalt() + rawPassword, user.getPasswordHash());
    }

    /**
     * Immutable value object carrying the generated salt and BCrypt hash from a hashing
     * operation.
     *
     * @param salt the Base64url-encoded random salt
     * @param hash the BCrypt-hashed {@code salt + password} value
     */
    public record PasswordHash(String salt, String hash) {
    }
}
