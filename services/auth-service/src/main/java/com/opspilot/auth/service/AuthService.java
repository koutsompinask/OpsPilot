package com.opspilot.auth.service;

import com.opspilot.auth.service.integration.TenantBootstrapRequest;
import com.opspilot.auth.service.integration.TenantClient;
import com.opspilot.auth.dto.InternalCreateUserRequest;
import com.opspilot.auth.dto.InternalCreateUserResponse;
import com.opspilot.auth.dto.LoginRequest;
import com.opspilot.auth.dto.RefreshTokenRequest;
import com.opspilot.auth.dto.RegisterRequest;
import com.opspilot.auth.dto.TokenResponse;
import com.opspilot.auth.domain.entity.AuthUser;
import com.opspilot.auth.domain.entity.RefreshSession;
import com.opspilot.auth.domain.entity.Role;
import com.opspilot.auth.exception.ConflictException;
import com.opspilot.auth.exception.UnauthorizedException;
import com.opspilot.auth.repository.AuthUserRepository;
import com.opspilot.auth.security.JwtService;
import jakarta.transaction.Transactional;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Core authentication orchestrator for the auth-service.
 *
 * <p>Handles tenant registration, user login, token refresh, and internal user creation.
 * Each operation that modifies state (register, refresh, createInternalUser) is wrapped in a
 * transaction to ensure atomicity between the credential write and the refresh session write.
 * Email addresses are normalised (lowercased and trimmed) before any lookup or storage to
 * prevent duplicate-account issues caused by casing differences.</p>
 *
 * <p>Key collaborators:</p>
 * <ul>
 *   <li>{@link PasswordHashService} — salt generation and PBKDF2/BCrypt hashing</li>
 *   <li>{@link JwtService} — access token issuance</li>
 *   <li>{@link RefreshTokenService} — refresh token issuance, validation, and revocation</li>
 *   <li>{@link TenantClient} — tenant bootstrap call during registration</li>
 * </ul>
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final AuthUserRepository authUserRepository;
    private final PasswordHashService passwordHashService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final TenantClient tenantClient;

    public AuthService(
            AuthUserRepository authUserRepository,
            PasswordHashService passwordHashService,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            TenantClient tenantClient
    ) {
        this.authUserRepository = authUserRepository;
        this.passwordHashService = passwordHashService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.tenantClient = tenantClient;
    }

    /**
     * Registers a new tenant and its initial admin user.
     *
     * <p>The operation is transactional. The tenant-service bootstrap call happens before the
     * credential is persisted: if the downstream call fails the local transaction is rolled back,
     * leaving no orphan records. UUIDs for both tenant and user are generated here so that the
     * same identifiers are used in both services.</p>
     *
     * @param request registration payload containing tenant name, admin display name, email, and
     *                password
     * @return a {@link TokenResponse} with a freshly issued access and refresh token pair
     * @throws ConflictException if the email address is already registered
     */
    @Transactional
    public TokenResponse register(RegisterRequest request) {
        // Normalise email (lowercase + trim) before any lookup or storage to prevent
        // duplicate accounts caused by casing differences
        String email = normalizeEmail(request.email());
        log.info("auth_register_requested email={} tenantName={}", email, request.tenantName());
        if (authUserRepository.existsByEmail(email)) {
            log.warn("auth_register_conflict email={}", email);
            throw new ConflictException("Email is already registered");
        }

        // Generate stable UUIDs up-front so both services reference the same identifiers
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // Bootstrap the tenant profile in tenant-service before writing the credential record
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

    /**
     * Authenticates a user by email and password and issues a token pair.
     *
     * <p>A generic "Invalid credentials" message is returned for both unknown email and wrong
     * password to prevent user enumeration attacks.</p>
     *
     * @param request login payload with email and plaintext password
     * @return a {@link TokenResponse} with access and refresh tokens
     * @throws UnauthorizedException if the email is not found, the password is incorrect, or the
     *                               account is inactive
     */
    public TokenResponse login(LoginRequest request) {
        // Normalise before lookup so casing differences don't prevent a valid login
        String email = normalizeEmail(request.email());
        AuthUser user = authUserRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("auth_login_failed email={} reason=user_not_found", email);
                    return new UnauthorizedException("Invalid credentials");
                });

        // Active check precedes the password comparison to avoid unnecessary hashing work
        if (!user.isActive() || !passwordHashService.matches(request.password(), user)) {
            log.warn("auth_login_failed email={} userId={} reason=inactive_or_bad_password", email, user.getId());
            throw new UnauthorizedException("Invalid credentials");
        }
        log.info("auth_login_succeeded email={} userId={} tenantId={}", email, user.getId(), user.getTenantId());

        return issueTokens(user, "login");
    }

    /**
     * Rotates a refresh token and issues a new token pair.
     *
     * <p>The existing refresh session is validated and then immediately revoked (single-use
     * rotation) before a new session is created. This ensures that a stolen refresh token cannot
     * be replayed even if it has not yet expired.</p>
     *
     * @param request payload containing the raw refresh token to exchange
     * @return a {@link TokenResponse} containing a new access token and a newly issued refresh
     *         token
     * @throws UnauthorizedException if the refresh token is invalid, expired, or revoked
     */
    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        RefreshSession existingSession = refreshTokenService.validate(request.refreshToken());
        // Revoke the old session immediately to enforce single-use semantics
        refreshTokenService.revoke(existingSession);
        log.info(
                "auth_refresh_succeeded userId={} tenantId={} sessionId={}",
                existingSession.getUser().getId(),
                existingSession.getUser().getTenantId(),
                existingSession.getId()
        );
        return issueTokens(existingSession.getUser(), "refresh");
    }

    /**
     * Creates an auth credential record for a user whose profile already exists in
     * tenant-service.
     *
     * <p>Called exclusively via {@link com.opspilot.auth.controller.InternalAuthController}
     * as part of the service-to-service member-provisioning flow. The caller is responsible for
     * providing the pre-assigned {@code userId} and {@code tenantId} so that both services
     * reference the same identifiers.</p>
     *
     * @param request payload with user ID, tenant ID, email, password, and role
     * @return an {@link InternalCreateUserResponse} echoing the saved user's identifiers and role
     * @throws ConflictException if the email is already registered in the auth store
     */
    @Transactional
    public InternalCreateUserResponse createInternalUser(InternalCreateUserRequest request) {
        // Normalise email before conflict check and storage
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
        PasswordHashService.PasswordHash passwordHash = passwordHashService.hash(password);
        AuthUser user = new AuthUser();
        user.setId(userId);
        user.setTenantId(tenantId);
        user.setEmail(email);
        user.setPasswordSalt(passwordHash.salt());
        user.setPasswordHash(passwordHash.hash());
        user.setRole(role);
        user.setActive(true);
        return user;
    }

    private TokenResponse issueTokens(AuthUser user, String metadata) {
        String accessToken = jwtService.issueAccessToken(user);
        // metadata records the issuance context (e.g. "login", "register", "refresh") in the
        // session row for audit / debugging purposes
        RefreshTokenService.IssuedRefreshToken refreshToken = refreshTokenService.issue(user, metadata);
        return new TokenResponse(
                accessToken,
                refreshToken.rawToken(),
                jwtService.accessTokenExpiresInSeconds(),
                "Bearer"
        );
    }

    private String normalizeEmail(String email) {
        // Lowercase using ROOT locale to avoid locale-specific case-folding surprises (e.g. Turkish 'i')
        return email.toLowerCase(Locale.ROOT).trim();
    }
}
