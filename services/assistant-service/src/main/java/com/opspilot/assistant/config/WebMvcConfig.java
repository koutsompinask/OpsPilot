package com.opspilot.assistant.config;

import com.opspilot.assistant.util.logging.UserContextMdcInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers MVC interceptors for the assistant-service.
 *
 * <p>Currently registers {@link UserContextMdcInterceptor}, which populates the SLF4J MDC
 * with user identity fields (userId, tenantId, userEmail) extracted from the authenticated
 * JWT, so that all log statements within a request carry that context automatically.</p>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final UserContextMdcInterceptor userContextMdcInterceptor;

    public WebMvcConfig(UserContextMdcInterceptor userContextMdcInterceptor) {
        this.userContextMdcInterceptor = userContextMdcInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userContextMdcInterceptor);
    }
}
