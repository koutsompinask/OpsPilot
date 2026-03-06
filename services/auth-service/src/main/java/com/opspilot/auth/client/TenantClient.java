package com.opspilot.auth.client;

import com.opspilot.auth.exception.UpstreamServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class TenantClient {

    private final RestClient restClient;
    private final String serviceToken;

    public TenantClient(
            RestClient.Builder builder,
            @Value("${tenant-service.base-url}") String tenantServiceBaseUrl,
            @Value("${auth.service-token}") String serviceToken
    ) {
        this.restClient = builder.baseUrl(tenantServiceBaseUrl).build();
        this.serviceToken = serviceToken;
    }

    public void bootstrapTenant(TenantBootstrapRequest request) {
        restClient
                .post()
                .uri("/internal/tenants/bootstrap")
                .header("X-Service-Token", serviceToken)
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new UpstreamServiceException("Tenant bootstrap failed with status " + res.getStatusCode());
                })
                .toBodilessEntity();
    }
}
