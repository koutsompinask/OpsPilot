package com.opspilot.tenant.service;

import com.opspilot.tenant.client.AuthClient;
import com.opspilot.tenant.dto.CreateUserRequest;
import com.opspilot.tenant.dto.InternalBootstrapTenantRequest;
import com.opspilot.tenant.dto.InternalCreateAuthUserRequest;
import com.opspilot.tenant.dto.TenantResponse;
import com.opspilot.tenant.dto.UpdateTenantRequest;
import com.opspilot.tenant.dto.UserResponse;
import com.opspilot.tenant.entity.Role;
import com.opspilot.tenant.entity.Tenant;
import com.opspilot.tenant.entity.UserProfile;
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
import org.springframework.stereotype.Service;

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

    @Transactional
    public void bootstrapTenant(InternalBootstrapTenantRequest request) {
        log.info(
                "tenant_bootstrap_requested tenantId={} adminUserId={} adminEmail={}",
                request.tenantId(),
                request.adminUserId(),
                normalizeEmail(request.adminEmail())
        );
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

    public TenantResponse getTenant(UUID tenantId) {
        log.info("tenant_get_requested tenantId={}", tenantId);
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException(TENANT_NOT_FOUND_MSG));
        log.info("tenant_get_succeeded tenantId={}", tenantId);
        return new TenantResponse(tenant.getId(), tenant.getName(), tenant.getSettingsJson());
    }

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

    public List<UserResponse> listUsers(UUID tenantId) {
        log.info("tenant_users_list_requested tenantId={}", tenantId);
        List<UserResponse> users = userProfileRepository.findAllByTenant_Id(tenantId).stream()
                .map(this::toUserResponse)
                .toList();
        log.info("tenant_users_list_succeeded tenantId={} userCount={}", tenantId, users.size());
        return users;
    }

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

        Role requestedRole = request.role() == null ? Role.TENANT_MEMBER : request.role();
        UUID userId = UUID.randomUUID();

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

    private String normalizeEmail(String email) {
        return email.toLowerCase(Locale.ROOT).trim();
    }
}
