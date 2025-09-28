package com.bookstack.bookstack.common.config;

import com.bookstack.bookstack.common.interceptor.RateLimitInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    @Autowired
    public WebMvcConfig(RateLimitInterceptor rateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
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