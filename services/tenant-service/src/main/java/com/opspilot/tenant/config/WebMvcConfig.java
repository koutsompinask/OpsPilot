package com.opspilot.tenant.config;

import com.opspilot.tenant.logging.UserContextMdcInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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
