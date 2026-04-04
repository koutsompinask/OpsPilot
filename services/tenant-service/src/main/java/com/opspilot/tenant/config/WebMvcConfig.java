package com.opspilot.tenant.config;

import com.opspilot.tenant.util.logging.UserContextMdcInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC configuration for the tenant-service.
 *
 * <p>Registers request interceptors that enrich the MDC with user context so that
 * log entries for authenticated requests automatically include the caller's identity.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final UserContextMdcInterceptor userContextMdcInterceptor;

    public WebMvcConfig(UserContextMdcInterceptor userContextMdcInterceptor) {
        this.userContextMdcInterceptor = userContextMdcInterceptor;
    }

    /**
     * Registers the {@link UserContextMdcInterceptor} so that user identity fields
     * ({@code userId}, {@code tenantId}, {@code userEmail}) are available in the MDC
     * for the duration of every handler invocation.
     *
     * @param registry the interceptor registry to add interceptors to
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userContextMdcInterceptor);
    }
}
