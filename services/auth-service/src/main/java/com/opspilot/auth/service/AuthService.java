package com.opspilot.auth.service;

import com.opspilot.auth.client.TenantBootstrapRequest;
import com.opspilot.auth.client.TenantClient;
import com.opspilot.auth.dto.InternalCreateUserRequest;
import com.opspilot.auth.dto.InternalCreateUserResponse;
import com.opspilot.auth.dto.LoginRequest;
import com.opspilot.auth.dto.RefreshTokenRequest;
import com.opspilot.auth.dto.RegisterRequest;
import com.opspilot.auth.dto.TokenResponse;
import com.opspilot.auth.entity.AuthUser;
import com.opspilot.auth.entity.RefreshSession;
import com.opspilot.auth.entity.Role;
import com.opspilot.auth.exception.ConflictException;
import com.opspilot.auth.exception.UnauthorizedException;
import com.opspilot.auth.repository.AuthUserRepository;
import com.opspilot.auth.security.JwtService;
import jakarta.transaction.Transactional;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final TenantClient tenantClient;

    public AuthService(
            AuthUserRepository authUserRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            TenantClient tenantClient
    ) {
        this.authUserRepository = authUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.tenantClient = tenantClient;
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (authUserRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already registered");
        }

        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        tenantClient.bootstrapTenant(new TenantBootstrapRequest(
                tenantId,
                request.tenantName(),
                userId,
                request.adminName(),
                email,
                Role.TENANT_ADMIN
        ));

        AuthUser user = createAuthUser(userId, tenantId, email, request.password(), Role.TENANT_ADMIN);
        authUserRepository.save(user);

        return issueTokens(user, "register");
    }

    public TokenResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        AuthUser user = authUserRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!user.isActive() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        return issueTokens(user, "login");
    }

    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        RefreshSession existingSession = refreshTokenService.validate(request.refreshToken());
        refreshTokenService.revoke(existingSession);
        return issueTokens(existingSession.getUser(), "refresh");
    }

    @Transactional
    public InternalCreateUserResponse createInternalUser(InternalCreateUserRequest request) {
        String email = normalizeEmail(request.email());
        if (authUserRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already registered");
        }

        AuthUser user = createAuthUser(request.userId(), request.tenantId(), email, request.password(), request.role());
        authUserRepository.save(user);
        return new InternalCreateUserResponse(user.getId(), user.getTenantId(), user.getEmail(), user.getRole());
    }

    private AuthUser createAuthUser(UUID userId, UUID tenantId, String email, String password, Role role) {
        AuthUser user = new AuthUser();
        user.setId(userId);
        user.setTenantId(tenantId);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setActive(true);
        return user;
    }

    private TokenResponse issueTokens(AuthUser user, String metadata) {
        String accessToken = jwtService.issueAccessToken(user);
        RefreshTokenService.IssuedRefreshToken refreshToken = refreshTokenService.issue(user, metadata);
        return new TokenResponse(
                accessToken,
                refreshToken.rawToken(),
                jwtService.accessTokenExpiresInSeconds(),
                "Bearer"
        );
    }

    private String normalizeEmail(String email) {
        return email.toLowerCase(Locale.ROOT).trim();
    }
}
