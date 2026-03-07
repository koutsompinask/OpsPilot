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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
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
        log.info("auth_register_requested email={} tenantName={}", email, request.tenantName());
        if (authUserRepository.existsByEmail(email)) {
            log.warn("auth_register_conflict email={}", email);
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
        log.info("auth_register_succeeded email={} userId={} tenantId={}", email, userId, tenantId);

        return issueTokens(user, "register");
    }

    public TokenResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        AuthUser user = authUserRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("auth_login_failed email={} reason=user_not_found", email);
                    return new UnauthorizedException("Invalid credentials");
                });

        if (!user.isActive() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("auth_login_failed email={} userId={} reason=inactive_or_bad_password", email, user.getId());
            throw new UnauthorizedException("Invalid credentials");
        }
        log.info("auth_login_succeeded email={} userId={} tenantId={}", email, user.getId(), user.getTenantId());

        return issueTokens(user, "login");
    }

    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        RefreshSession existingSession = refreshTokenService.validate(request.refreshToken());
        refreshTokenService.revoke(existingSession);
        log.info(
                "auth_refresh_succeeded userId={} tenantId={} sessionId={}",
                existingSession.getUser().getId(),
                existingSession.getUser().getTenantId(),
                existingSession.getId()
        );
        return issueTokens(existingSession.getUser(), "refresh");
    }

    @Transactional
    public InternalCreateUserResponse createInternalUser(InternalCreateUserRequest request) {
        String email = normalizeEmail(request.email());
        log.info("auth_internal_create_user_requested email={} userId={} tenantId={}", email, request.userId(), request.tenantId());
        if (authUserRepository.existsByEmail(email)) {
            log.warn("auth_internal_create_user_conflict email={}", email);
            throw new ConflictException("Email is already registered");
        }

        AuthUser user = createAuthUser(request.userId(), request.tenantId(), email, request.password(), request.role());
        authUserRepository.save(user);
        log.info("auth_internal_create_user_succeeded email={} userId={} tenantId={}", email, user.getId(), user.getTenantId());
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
