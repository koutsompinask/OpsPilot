package com.opspilot.auth.service;

import com.opspilot.auth.domain.entity.AuthUser;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordHashService {

    private static final int SALT_BYTES = 24;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;

    public PasswordHashService(PasswordEncoder passwordEncoder, SecureRandom secureRandom) {
        this.passwordEncoder = passwordEncoder;
        this.secureRandom = secureRandom;
    }

    public PasswordHash hash(String rawPassword) {
        byte[] saltBytes = new byte[SALT_BYTES];
        secureRandom.nextBytes(saltBytes);
        String salt = Base64.getUrlEncoder().withoutPadding().encodeToString(saltBytes);
        String passwordHash = passwordEncoder.encode(salt + rawPassword);
        return new PasswordHash(salt, passwordHash);
    }

    public boolean matches(String rawPassword, AuthUser user) {
        return passwordEncoder.matches(user.getPasswordSalt() + rawPassword, user.getPasswordHash());
    }

    public record PasswordHash(String salt, String hash) {
    }
}
