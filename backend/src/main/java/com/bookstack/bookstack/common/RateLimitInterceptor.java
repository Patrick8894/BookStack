package com.bookstack.bookstack.common;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.lang.NonNull;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final CacheManager cacheManager;
    
    // In-memory fallback cache if the CacheManager is not available
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    
    @Value("${rate.limit.enabled:true}")
    private boolean enabled;
    
    @Value("${rate.limit.requests-per-minute:20}")
    private int requestsPerMinute;
    
    public RateLimitInterceptor(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        if (!enabled) return true;

        String clientIp = getClientIp(request);
        
        // Get or create a rate limit bucket for this client
        Bucket bucket = resolveBucket(clientIp);

        // Try to consume a token from the bucket
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        
        // If successful, add rate limit headers to the response
        if (probe.isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            response.addHeader("X-Rate-Limit-Reset", String.valueOf(probe.getNanosToWaitForRefill() / 1_000_000_000));
            return true;
        }

        // If rate limit exceeded, return 429 Too Many Requests
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(probe.getNanosToWaitForRefill() / 1_000_000_000));
        response.getWriter().write("Rate limit exceeded. Try again later.");
        return false;
    }
    
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Take first IP if X-Forwarded-For contains multiple IPs
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
    
    private Bucket resolveBucket(String key) {
        // Try to get the bucket from the cache first
        Cache cache = cacheManager.getCache("rate-limit-buckets");
        if (cache != null) {
            return cache.get(key, () -> createNewBucket());
        }
        
        // Fall back to in-memory map if cache is not available
        return buckets.computeIfAbsent(key, k -> createNewBucket());
    }
    
    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.classic(requestsPerMinute, 
                Refill.greedy(requestsPerMinute, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }
}