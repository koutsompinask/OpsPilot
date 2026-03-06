package com.opspilot.tenant.client;

import com.opspilot.tenant.dto.InternalCreateAuthUserRequest;
import com.opspilot.tenant.dto.InternalCreateAuthUserResponse;
import com.opspilot.tenant.exception.UpstreamServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AuthClient {

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
        return restClient
                .post()
                .uri("/internal/auth/users")
                .header("X-Service-Token", serviceToken)
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new UpstreamServiceException("Auth user creation failed with status " + res.getStatusCode());
                })
                .body(InternalCreateAuthUserResponse.class);
    }
}
