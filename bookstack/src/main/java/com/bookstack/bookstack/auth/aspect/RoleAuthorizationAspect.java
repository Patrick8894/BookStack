package com.bookstack.bookstack.auth.aspect;

import java.util.Arrays;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.bookstack.bookstack.auth.annotation.RequireRole;
import com.bookstack.bookstack.auth.service.JwtService;
import com.bookstack.bookstack.common.exception.UnauthorizedException;
import com.bookstack.bookstack.common.exception.ForbiddenException;

import jakarta.servlet.http.HttpServletRequest;

@Aspect
@Component
public class RoleAuthorizationAspect {
    
    private final JwtService jwtService;
    
    public RoleAuthorizationAspect(JwtService jwtService) {
        this.jwtService = jwtService;
    }
    
    @Around("@annotation(requireRole)")
    public Object authorize(ProceedingJoinPoint joinPoint, RequireRole requireRole) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Authentication required");
        }
        
        String token = authHeader.substring(7);
        
        // Validate token
        if (!jwtService.isTokenValid(token)) {
            throw new UnauthorizedException("Invalid or expired token");
        }
        
        String userRole = jwtService.extractRole(token);
        
        if (userRole == null || !Arrays.asList(requireRole.value()).contains(userRole)) {
            throw new ForbiddenException("Insufficient permissions. Required roles: " + Arrays.toString(requireRole.value()));
        }
        
        return joinPoint.proceed();
    }
}