package com.opspilot.ticket.config;

import com.opspilot.ticket.util.logging.UserContextMdcInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC configuration for the ticket-service.
 *
 * <p>Registers the {@link UserContextMdcInterceptor} so that authenticated user context
 * (user ID, tenant ID, role) is available in the SLF4J MDC for every request's log output.</p>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final UserContextMdcInterceptor userContextMdcInterceptor;

    public WebMvcConfig(UserContextMdcInterceptor userContextMdcInterceptor) {
        this.userContextMdcInterceptor = userContextMdcInterceptor;
    }

    /**
     * Registers interceptors that run around every MVC handler invocation.
     *
     * @param registry the interceptor registry to add interceptors to
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userContextMdcInterceptor);
    }
}
