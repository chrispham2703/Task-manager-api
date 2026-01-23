package com.taskmanager.api.security;

import com.taskmanager.api.config.RateLimitConfig;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitConfig rateLimitConfig;

    public RateLimitFilter(RateLimitConfig rateLimitConfig) {
        this.rateLimitConfig = rateLimitConfig;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String clientIP = getClientIP(request);
        String path = request.getRequestURI();
        Bucket bucket;

        // Use stricter rate limit for auth endpoints
        if (path.startsWith("/api/auth")) {
            bucket = rateLimitConfig.resolveAuthBucket(clientIP);
        } else if (path.startsWith("/api/")) {
            // For authenticated endpoints, use user-based key if available
            String authHeader = request.getHeader("Authorization");
            String key = (authHeader != null) ? authHeader.hashCode() + "" : clientIP;
            bucket = rateLimitConfig.resolveApiBucket(key);
        } else {
            // No rate limiting for other endpoints (actuator, swagger, etc.)
            filterChain.doFilter(request, response);
            return;
        }

        if (bucket.tryConsume(1)) {
            // Add rate limit headers
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(bucket.getAvailableTokens()));
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many requests\",\"message\":\"Rate limit exceeded. Please try again later.\"}");
        }
    }

    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
