package com.opspilot.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.opspilot.auth.domain.entity.AuthUser;
import java.security.SecureRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PasswordHashServiceTest {

    private PasswordHashService passwordHashService;

    @BeforeEach
    void setUp() {
        passwordHashService = new PasswordHashService(new BCryptPasswordEncoder(), new SecureRandom());
    }

    @Test
    void hashProducesSaltedPasswordThatMatchesOriginalInput() {
        PasswordHashService.PasswordHash passwordHash = passwordHashService.hash("Password123");

        AuthUser user = new AuthUser();
        user.setPasswordSalt(passwordHash.salt());
        user.setPasswordHash(passwordHash.hash());

        assertThat(passwordHash.salt()).isNotBlank();
        assertThat(passwordHash.hash()).isNotBlank().isNotEqualTo("Password123");
        assertThat(passwordHashService.matches("Password123", user)).isTrue();
        assertThat(passwordHashService.matches("WrongPassword123", user)).isFalse();
    }
}
