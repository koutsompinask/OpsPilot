package com.opspilot.assistant.util.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * MVC interceptor that enriches the MDC with user identity fields extracted from the JWT,
 * making every log statement in a request context automatically tagged with the user and tenant.
 *
 * Runs after Spring Security has authenticated the request. Extracts {@code sub} (userId),
 * {@code tenant_id}, and {@code email} claims from the {@link Jwt} principal and writes them
 * to MDC keys {@code userId}, {@code tenantId}, and {@code userEmail} respectively.
 * The entries are removed in {@code afterCompletion} to prevent MDC leakage.
 */
@Component
public class UserContextMdcInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            if (jwt.getSubject() != null) {
                MDC.put("userId", jwt.getSubject());
            }
            String tenantId = jwt.getClaimAsString("tenant_id");
            if (tenantId != null && !tenantId.isBlank()) {
                MDC.put("tenantId", tenantId);
            }
            String email = jwt.getClaimAsString("email");
            if (email != null && !email.isBlank()) {
                MDC.put("userEmail", email);
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler,
            Exception ex
    ) {
        MDC.remove("userId");
        MDC.remove("tenantId");
        MDC.remove("userEmail");
    }
}
