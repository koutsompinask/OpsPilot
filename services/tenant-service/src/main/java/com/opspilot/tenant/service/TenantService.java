package com.opspilot.tenant.service;

import com.opspilot.tenant.service.integration.AuthClient;
import com.opspilot.tenant.dto.CreateUserRequest;
import com.opspilot.tenant.dto.InternalBootstrapTenantRequest;
import com.opspilot.tenant.dto.InternalCreateAuthUserRequest;
import com.opspilot.tenant.dto.TenantResponse;
import com.opspilot.tenant.dto.UpdateTenantRequest;
import com.opspilot.tenant.dto.UserResponse;
import com.opspilot.tenant.domain.entity.Role;
import com.opspilot.tenant.domain.entity.Tenant;
import com.opspilot.tenant.domain.entity.UserProfile;
import com.opspilot.tenant.exception.ConflictException;
import com.opspilot.tenant.exception.NotFoundException;
import com.opspilot.tenant.repository.TenantRepository;
import com.opspilot.tenant.repository.UserProfileRepository;
import com.opspilot.tenant.security.CurrentUser;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Core business-logic service for tenant and user-profile management.
 *
 * <p>This service owns two distinct flows:
 * <ol>
 *   <li><b>Bootstrap</b> — called internally by auth-service when a new tenant registers.
 *       Creates the {@link Tenant} record and the initial admin {@link UserProfile} in one
 *       transaction. The user ID and tenant ID are assigned by auth-service before this call.</li>
 *   <li><b>User creation</b> — called by tenant admins to invite additional users. Generates
 *       a new UUID for the user, calls auth-service to provision the credential account first
 *       (bi-directional provisioning), then persists the local {@link UserProfile}. Both steps
 *       run inside a single transaction so a failure in auth-service rolls back the profile save.</li>
 * </ol>
 */
@Service
public class TenantService {

    private static final String TENANT_NOT_FOUND_MSG = "Tenant not found";
    private static final Logger log = LoggerFactory.getLogger(TenantService.class);
    private final TenantRepository tenantRepository;
    private final UserProfileRepository userProfileRepository;
    private final AuthClient authClient;

    public TenantService(TenantRepository tenantRepository, UserProfileRepository userProfileRepository, AuthClient authClient) {
        this.tenantRepository = tenantRepository;
        this.userProfileRepository = userProfileRepository;
        this.authClient = authClient;
    }

    /**
     * Creates the tenant record and initial admin profile as part of the registration bootstrap.
     *
     * <p>This method is called by {@link com.opspilot.tenant.controller.InternalTenantController}
     * on behalf of auth-service. Because the new tenant's JWT does not exist yet, auth-service
     * acts as a trusted orchestrator: it creates the credential record, then calls this endpoint
     * with the pre-assigned tenant and user IDs. Both the {@link Tenant} and admin
     * {@link UserProfile} are persisted in a single transaction.
     *
     * @param request tenant and admin-user details forwarded by auth-service
     * @throws ConflictException if a tenant with the given ID already exists
     */
    @Transactional
    public void bootstrapTenant(InternalBootstrapTenantRequest request) {
        log.info(
                "tenant_bootstrap_requested tenantId={} adminUserId={} adminEmail={}",
                request.tenantId(),
                request.adminUserId(),
                normalizeEmail(request.adminEmail())
        );
        // Guard against duplicate bootstrap calls (e.g. auth-service retry on a transient error)
        if (tenantRepository.existsById(request.tenantId())) {
            log.warn("tenant_bootstrap_conflict tenantId={}", request.tenantId());
            throw new ConflictException("Tenant already exists");
        }

        Tenant tenant = new Tenant();
        tenant.setId(request.tenantId());
        tenant.setName(request.tenantName());
        tenantRepository.save(tenant);

        UserProfile admin = new UserProfile();
        admin.setUserId(request.adminUserId());
        admin.setTenant(tenant);
        admin.setDisplayName(request.adminName());
        admin.setEmail(normalizeEmail(request.adminEmail()));
        admin.setRole(request.role());
        userProfileRepository.save(admin);
        log.info("tenant_bootstrap_succeeded tenantId={} adminUserId={}", tenant.getId(), admin.getUserId());
    }

    /**
     * Retrieves the tenant profile for the given tenant ID.
     *
     * @param tenantId the ID of the tenant to look up
     * @return the tenant's profile including name and settings JSON
     * @throws NotFoundException if no tenant with the given ID exists
     */
    public TenantResponse getTenant(UUID tenantId) {
        log.info("tenant_get_requested tenantId={}", tenantId);
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException(TENANT_NOT_FOUND_MSG));
        log.info("tenant_get_succeeded tenantId={}", tenantId);
        return new TenantResponse(tenant.getId(), tenant.getName(), tenant.getSettingsJson());
    }

    /**
     * Updates the name and settings for the given tenant.
     *
     * <p>The caller's tenant ID is taken from the JWT in the controller and passed directly here,
     * ensuring a user can never modify a different tenant's profile.
     *
     * @param tenantId the ID of the tenant to update
     * @param request  the new name and optional settings JSON
     * @return the updated tenant profile
     * @throws NotFoundException if no tenant with the given ID exists
     */
    @Transactional
    public TenantResponse updateTenant(UUID tenantId, UpdateTenantRequest request) {
        log.info("tenant_update_requested tenantId={} name={}", tenantId, request.name());
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException(TENANT_NOT_FOUND_MSG));
        tenant.setName(request.name());
        tenant.setSettingsJson(request.settingsJson());
        tenantRepository.save(tenant);
        log.info("tenant_update_succeeded tenantId={}", tenantId);
        return new TenantResponse(tenant.getId(), tenant.getName(), tenant.getSettingsJson());
    }

    /**
     * Returns a page of user profiles belonging to the given tenant.
     *
     * <p>Tenant isolation is enforced by the repository query: {@code findAllByTenant_Id}
     * generates a {@code WHERE tenant_id = ?} clause, so only profiles linked to the
     * caller's own tenant are ever returned.
     *
     * @param tenantId the ID of the tenant whose users should be listed
     * @param pageable pagination and sort parameters supplied by the caller
     * @return a page of user profiles for the tenant
     */
    public Page<UserResponse> listUsers(UUID tenantId, Pageable pageable) {
        log.info("tenant_users_list_requested tenantId={} page={} size={}", tenantId, pageable.getPageNumber(), pageable.getPageSize());
        // tenant_id filter is applied at query time — no cross-tenant data can leak through
        Page<UserResponse> users = userProfileRepository.findAllByTenant_Id(tenantId, pageable)
                .map(this::toUserResponse);
        log.info("tenant_users_list_succeeded tenantId={} userCount={} totalElements={}", tenantId, users.getNumberOfElements(), users.getTotalElements());
        return users;
    }

    /**
     * Creates a new user within the actor's tenant, provisioning both auth and profile records.
     *
     * <p>The UUID for the new user is generated here and shared with auth-service in the same
     * call, keeping the primary key consistent across services. The sequence is:
     * <ol>
     *   <li>Generate a new {@code userId}</li>
     *   <li>Call auth-service via {@link AuthClient} to create the credential account — if this
     *       fails, an {@link com.opspilot.tenant.exception.UpstreamServiceException} is thrown
     *       and the transaction rolls back before the profile is persisted</li>
     *   <li>Persist the local {@link UserProfile}</li>
     * </ol>
     *
     * @param actor   the authenticated admin performing the operation; provides the tenant scope
     * @param request the new user's display name, email, password, and optional role
     * @return the newly created user profile
     * @throws NotFoundException if the actor's tenant record no longer exists
     */
    @Transactional
    public UserResponse createUser(CurrentUser actor, CreateUserRequest request) {
        log.info(
                "tenant_user_create_requested actorUserId={} tenantId={} email={} requestedRole={}",
                actor.userId(),
                actor.tenantId(),
                normalizeEmail(request.email()),
                request.role()
        );
        Tenant tenant = tenantRepository.findById(actor.tenantId())
                .orElseThrow(() -> new NotFoundException(TENANT_NOT_FOUND_MSG));

        // Default role to TENANT_MEMBER when the caller omits the field
        Role requestedRole = request.role() == null ? Role.TENANT_MEMBER : request.role();
        // Generate the UUID here so that both auth-service and tenant-service share the same user ID
        UUID userId = UUID.randomUUID();

        // Bi-directional provisioning: auth-service must create the credential record first;
        // if it fails the enclosing transaction will roll back and no profile will be saved
        authClient.createUser(new InternalCreateAuthUserRequest(
                userId,
                actor.tenantId(),
                normalizeEmail(request.email()),
                request.password(),
                requestedRole
        ));

        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setTenant(tenant);
        profile.setDisplayName(request.displayName());
        profile.setEmail(normalizeEmail(request.email()));
        profile.setRole(requestedRole);
        userProfileRepository.save(profile);
        log.info(
                "tenant_user_create_succeeded actorUserId={} tenantId={} createdUserId={} role={}",
                actor.userId(),
                actor.tenantId(),
                profile.getUserId(),
                profile.getRole()
        );

        return toUserResponse(profile);
    }

    private UserResponse toUserResponse(UserProfile profile) {
        return new UserResponse(
                profile.getUserId(),
                profile.getTenant().getId(),
                profile.getDisplayName(),
                profile.getEmail(),
                profile.getRole()
        );
    }

    /** Normalises an email address to lowercase with surrounding whitespace removed. */
    private String normalizeEmail(String email) {
        return email.toLowerCase(Locale.ROOT).trim();
    }
}
