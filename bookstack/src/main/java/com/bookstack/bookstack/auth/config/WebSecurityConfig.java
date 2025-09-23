package com.bookstack.bookstack.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
public class WebSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)  // Modern way to disable CSRF
            .exceptionHandling(exc -> exc
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/auth/**", 
                    "/swagger-ui/**", 
                    "/swagger-ui.html", 
                    "/v3/api-docs/**",
                    "/graphql/**",        // Allow all paths under /graphql
                    "/graphiql/**"
                ).permitAll()  // public: register & login
                .anyRequest().authenticated()            // everything else requires JWT
            );
        return http.build();
    }
}