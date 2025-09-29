package com.bookstack.bookstack.common.config;

import com.bookstack.bookstack.common.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.lang.NonNull;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    public WebMvcConfig(RateLimitInterceptor rateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/**")  // Apply to all endpoints
                .excludePathPatterns(
                    "/api/auth/**",      // Exclude auth endpoints
                    "/swagger-ui/**",    // Exclude Swagger UI
                    "/v3/api-docs/**",   // Exclude OpenAPI docs
                    "/favicon.ico",      // Exclude favicon
                    "/static/**",        // Exclude static resources
                    "/css/**",           // Exclude CSS
                    "/js/**",            // Exclude JavaScript
                    "/images/**"         // Exclude images
                );
    }
}