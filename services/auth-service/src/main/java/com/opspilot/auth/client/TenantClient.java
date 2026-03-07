package com.opspilot.auth.client;

import com.opspilot.auth.exception.UpstreamServiceException;
import com.opspilot.auth.logging.RequestCorrelation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestBodySpec;

@Component
public class TenantClient {

    private static final Logger log = LoggerFactory.getLogger(TenantClient.class);
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
        String requestId = MDC.get(RequestCorrelation.MDC_KEY);
        log.info("tenant_client_bootstrap_request tenantId={} adminUserId={}", request.tenantId(), request.adminUserId());
        RequestBodySpec requestSpec = restClient
                .post()
                .uri("/internal/tenants/bootstrap")
                .header("X-Service-Token", serviceToken);
        if (requestId != null && !requestId.isBlank()) {
            requestSpec.header(RequestCorrelation.HEADER_NAME, requestId);
        }
        requestSpec.body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    log.error(
                            "tenant_client_bootstrap_failed tenantId={} status={}",
                            request.tenantId(),
                            res.getStatusCode()
                    );
                    throw new UpstreamServiceException("Tenant bootstrap failed with status " + res.getStatusCode());
                })
                .toBodilessEntity();
        log.info("tenant_client_bootstrap_succeeded tenantId={}", request.tenantId());
    }
}
