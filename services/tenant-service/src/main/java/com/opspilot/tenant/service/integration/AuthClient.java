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

    public InternalCreateAuthUserResponse createUser(InternalCreateAuthUserRequest request) {
        String requestId = MDC.get(RequestCorrelation.MDC_KEY);
        log.info("auth_client_create_user_request userId={} tenantId={} email={}", request.userId(), request.tenantId(), request.email());
        RequestBodySpec requestSpec = restClient
                .post()
                .uri("/internal/auth/users")
                .header("X-Service-Token", serviceToken);
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
                    throw new UpstreamServiceException("Auth user creation failed with status " + res.getStatusCode());
                })
                .body(InternalCreateAuthUserResponse.class);
        log.info("auth_client_create_user_succeeded userId={} tenantId={}", request.userId(), request.tenantId());
        return response;
    }
}
