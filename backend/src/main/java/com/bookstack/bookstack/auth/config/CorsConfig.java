package com.bookstack.bookstack.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // Allow all origins in development
        config.addAllowedOrigin("http://localhost:3000");
        config.addAllowedOrigin("http://127.0.0.1:3000");
        
        // If you're using Docker networking, also allow the Docker container hostnames
        config.addAllowedOrigin("http://frontend:3000");
        
        // Allow all HTTP methods
        config.addAllowedMethod("*");
        
        // Allow all headers
        config.addAllowedHeader("*");
        
        // Allow credentials (cookies, authorization headers, etc.)
        config.setAllowCredentials(true);
        
        // Cache preflight requests for 1 hour (3600 seconds)
        config.setMaxAge(3600L);
        
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}