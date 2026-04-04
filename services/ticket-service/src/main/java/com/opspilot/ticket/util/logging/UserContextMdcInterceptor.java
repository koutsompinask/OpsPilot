package com.opspilot.ticket.util.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * MVC interceptor that enriches MDC with user identity fields from the JWT, so every log
 * statement in a request is automatically tagged with the user, tenant, and role.
 *
 * Extracts {@code sub} (userId), {@code tenantId}, and {@code role} claims and writes them
 * to the corresponding MDC keys. Cleaned up in {@code afterCompletion} to prevent MDC
 * leakage across pooled threads.
 */
@Component
public class UserContextMdcInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            putIfPresent("userId", jwt.getSubject());
            putIfPresent("tenantId", jwt.getClaimAsString("tenantId"));
            putIfPresent("role", jwt.getClaimAsString("role"));
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        MDC.remove("userId");
        MDC.remove("tenantId");
        MDC.remove("role");
    }

    private void putIfPresent(String key, String value) {
        if (value != null && !value.isBlank()) {
            MDC.put(key, value);
        }
    }
}
