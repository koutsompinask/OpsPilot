package com.opspilot.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.opspilot.auth.domain.entity.AuthUser;
import com.opspilot.auth.domain.entity.RefreshSession;
import com.opspilot.auth.domain.entity.Role;
import com.opspilot.auth.dto.InternalCreateUserRequest;
import com.opspilot.auth.dto.LoginRequest;
import com.opspilot.auth.dto.RegisterRequest;
import com.opspilot.auth.exception.UnauthorizedException;
import com.opspilot.auth.repository.AuthUserRepository;
import com.opspilot.auth.security.JwtService;
import com.opspilot.auth.service.integration.TenantBootstrapRequest;
import com.opspilot.auth.service.integration.TenantClient;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthUserRepository authUserRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private TenantClient tenantClient;

    private AuthService authService;
    private PasswordHashService passwordHashService;

    @BeforeEach
    void setUp() {
        passwordHashService = new PasswordHashService(new BCryptPasswordEncoder(), new SecureRandom());
        authService = new AuthService(authUserRepository, passwordHashService, jwtService, refreshTokenService, tenantClient);
    }

    @Test
    void registerStoresSaltedHash() {
        when(authUserRepository.existsByEmail("admin@example.com")).thenReturn(false);
        when(authUserRepository.save(any(AuthUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubIssuedTokens("register");

        authService.register(new RegisterRequest("Acme", "Admin", "ADMIN@EXAMPLE.COM", "Password123"));

        ArgumentCaptor<AuthUser> userCaptor = ArgumentCaptor.forClass(AuthUser.class);
        verify(authUserRepository).save(userCaptor.capture());
        AuthUser savedUser = userCaptor.getValue();

        assertThat(savedUser.getEmail()).isEqualTo("admin@example.com");
        assertThat(savedUser.getPasswordSalt()).isNotBlank();
        assertThat(savedUser.getPasswordHash()).isNotBlank().isNotEqualTo("Password123");
        assertThat(passwordHashService.matches("Password123", savedUser)).isTrue();
        verify(tenantClient).bootstrapTenant(any(TenantBootstrapRequest.class));
    }

    @Test
    void createInternalUserGeneratesDistinctSaltPerUser() {
        when(authUserRepository.existsByEmail("user-one@example.com")).thenReturn(false);
        when(authUserRepository.existsByEmail("user-two@example.com")).thenReturn(false);
        when(authUserRepository.save(any(AuthUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.createInternalUser(new InternalCreateUserRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "user-one@example.com",
                "Password123",
                Role.TENANT_MEMBER
        ));
        authService.createInternalUser(new InternalCreateUserRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "user-two@example.com",
                "Password123",
                Role.TENANT_MEMBER
        ));

        ArgumentCaptor<AuthUser> userCaptor = ArgumentCaptor.forClass(AuthUser.class);
        verify(authUserRepository, times(2)).save(userCaptor.capture());
        AuthUser firstUser = userCaptor.getAllValues().get(0);
        AuthUser secondUser = userCaptor.getAllValues().get(1);

        assertThat(firstUser.getPasswordSalt()).isNotBlank();
        assertThat(secondUser.getPasswordSalt()).isNotBlank();
        assertThat(firstUser.getPasswordSalt()).isNotEqualTo(secondUser.getPasswordSalt());
        assertThat(passwordHashService.matches("Password123", firstUser)).isTrue();
        assertThat(passwordHashService.matches("Password123", secondUser)).isTrue();
    }

    @Test
    void loginAcceptsPasswordUsingStoredSalt() {
        AuthUser user = createUser("member@example.com", "Password123");
        when(authUserRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        stubIssuedTokens("login");

        authService.login(new LoginRequest("MEMBER@EXAMPLE.COM", "Password123"));

        verify(refreshTokenService).issue(eq(user), eq("login"));
    }

    @Test
    void loginRejectsWrongPassword() {
        AuthUser user = createUser("member@example.com", "Password123");
        when(authUserRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("member@example.com", "wrong-password")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid credentials");
    }

    private AuthUser createUser(String email, String password) {
        PasswordHashService.PasswordHash passwordHash = passwordHashService.hash(password);
        AuthUser user = new AuthUser();
        user.setId(UUID.randomUUID());
        user.setTenantId(UUID.randomUUID());
        user.setEmail(email);
        user.setPasswordSalt(passwordHash.salt());
        user.setPasswordHash(passwordHash.hash());
        user.setRole(Role.TENANT_MEMBER);
        user.setActive(true);
        return user;
    }

    private void stubIssuedTokens(String metadata) {
        when(jwtService.issueAccessToken(any(AuthUser.class))).thenReturn("access-token");
        when(jwtService.accessTokenExpiresInSeconds()).thenReturn(900L);

        RefreshSession refreshSession = new RefreshSession();
        refreshSession.setId(UUID.randomUUID());
        when(refreshTokenService.issue(any(AuthUser.class), eq(metadata)))
                .thenReturn(new RefreshTokenService.IssuedRefreshToken("refresh-token", refreshSession));
    }
}
