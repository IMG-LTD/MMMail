package com.mmmail.server.config;

import com.mmmail.server.access.V21ApiAccessGateInterceptor;
import com.mmmail.server.security.AuthorizationAnnotationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final V21ApiAccessGateInterceptor v21ApiAccessGateInterceptor;
    private final OrgProductAccessInterceptor orgProductAccessInterceptor;
    private final AuthorizationAnnotationInterceptor authorizationAnnotationInterceptor;

    public WebMvcConfig(
            V21ApiAccessGateInterceptor v21ApiAccessGateInterceptor,
            OrgProductAccessInterceptor orgProductAccessInterceptor,
            AuthorizationAnnotationInterceptor authorizationAnnotationInterceptor
    ) {
        this.v21ApiAccessGateInterceptor = v21ApiAccessGateInterceptor;
        this.orgProductAccessInterceptor = orgProductAccessInterceptor;
        this.authorizationAnnotationInterceptor = authorizationAnnotationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(v21ApiAccessGateInterceptor)
                .addPathPatterns("/api/v2/**");
        registry.addInterceptor(authorizationAnnotationInterceptor)
                .addPathPatterns("/api/v1/**", "/api/v2/**");
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
                        "/api/v2/platform/capabilities",
                        "/api/v2/share/**",
                        "/api/v2/public-share/**",
                        "/api/v2/system/health",
                        "/api/v2/system/status",
                        "/actuator/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**"
                );
    }
}
