package com.opspilot.tenant.service.integration;

import com.opspilot.tenant.dto.InternalCreateAuthUserRequest;
import com.opspilot.tenant.dto.InternalCreateAuthUserResponse;
import com.opspilot.tenant.exception.UpstreamServiceException;
import com.opspilot.tenant.util.logging.RequestCorrelation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestBodySpec;

/**
 * HTTP client for calling auth-service internal endpoints from tenant-service.
 *
 * <p>Used exclusively during the user-creation flow: tenant-service needs auth-service to
 * persist credential records (password hash, refresh-token store) before it saves its own
 * {@link com.opspilot.tenant.domain.entity.UserProfile}. The same shared service token that
 * guards {@link com.opspilot.tenant.controller.InternalTenantController} is reused here to
 * authenticate outbound calls to auth-service's internal endpoints.
 *
 * <p>The current request correlation ID is forwarded so that distributed traces can be
 * joined across both services in the logs.
 */
@Component
public class AuthClient {

    private static final Logger log = LoggerFactory.getLogger(AuthClient.class);
    private final RestClient restClient;
    private final String serviceToken;

    public AuthClient(
            RestClient.Builder builder,
            @Value("${auth-service.base-url}") String authServiceBaseUrl,
            @Value("${tenant.service-token}") String serviceToken
    ) {
        this.restClient = builder.baseUrl(authServiceBaseUrl).build();
        this.serviceToken = serviceToken;
    }

    /**
     * Calls auth-service to create a new credential account for a tenant user.
     *
     * <p>The {@code X-Service-Token} header authenticates the request on the auth-service side.
     * If a correlation ID is present in the MDC it is forwarded via {@code X-Request-Id} so
     * the call can be traced across both services. Any non-2xx response causes an
     * {@link UpstreamServiceException} which the caller's transaction will treat as a failure
     * and roll back.
     *
     * @param request the new user's identity and credentials to provision in auth-service
     * @return the auth-service response confirming the created account details
     * @throws UpstreamServiceException if auth-service returns an error HTTP status
     */
    public InternalCreateAuthUserResponse createUser(InternalCreateAuthUserRequest request) {
        String requestId = MDC.get(RequestCorrelation.MDC_KEY);
        log.info("auth_client_create_user_request userId={} tenantId={} email={}", request.userId(), request.tenantId(), request.email());
        RequestBodySpec requestSpec = restClient
                .post()
                .uri("/internal/auth/users")
                // Shared service token — same secret used by InternalTenantController on the inbound side
                .header("X-Service-Token", serviceToken);
        // Propagate the correlation ID from the inbound request to keep distributed traces joinable
        if (requestId != null && !requestId.isBlank()) {
            requestSpec.header(RequestCorrelation.HEADER_NAME, requestId);
        }
        InternalCreateAuthUserResponse response = requestSpec.body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    log.error(
                            "auth_client_create_user_failed userId={} tenantId={} status={}",
                            request.userId(),
                            request.tenantId(),
                            res.getStatusCode()
                    );
                    // Throw a typed exception so TenantService can surface a 502 and roll back the transaction
                    throw new UpstreamServiceException("Auth user creation failed with status " + res.getStatusCode());
                })
                .body(InternalCreateAuthUserResponse.class);
        log.info("auth_client_create_user_succeeded userId={} tenantId={}", request.userId(), request.tenantId());
        return response;
    }
}
