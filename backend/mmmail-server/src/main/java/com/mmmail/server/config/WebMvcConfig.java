package com.mmmail.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final OrgProductAccessInterceptor orgProductAccessInterceptor;

    public WebMvcConfig(OrgProductAccessInterceptor orgProductAccessInterceptor) {
        this.orgProductAccessInterceptor = orgProductAccessInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(orgProductAccessInterceptor)
                .addPathPatterns("/api/v1/**", "/api/v2/**")
                .excludePathPatterns(
                        "/api/v1/auth/**",
                        "/api/v1/orgs/**",
                        "/api/v1/public/**",
                        "/api/v1/settings/**",
                        "/api/v1/suite/**",
                        "/api/v1/audit/**",
                        "/api/v2/auth/**",
                        "/api/v2/public/**",
                        "/api/v2/public-share/**",
                        "/actuator/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**"
                );
    }
}
