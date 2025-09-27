package com.bookstack.bookstack.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.Collections;

@Configuration
public class WebSecurityConfig {

    @Bean
    @Profile("!dev")
    public SecurityFilterChain securedFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configure(http)) // Enable CORS
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/auth/**",
                    "/hello",
                    "/swagger-ui/**", 
                    "/swagger-ui.html", 
                    "/v3/api-docs/**",
                    "/graphql/**",
                    "/graphiql/**"
                ).permitAll()
                // All other endpoints will be handled by @RequireRole annotation
                .anyRequest().permitAll()
            );
        
        return http.build();
    }

    @Bean
    @Profile("dev")
    public SecurityFilterChain unsecuredFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configure(http)) // Enable CORS
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );
        return http.build();
    }
}