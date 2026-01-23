package com.taskmanager.api.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitConfig {

    private final Map<String, Bucket> authBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> apiBuckets = new ConcurrentHashMap<>();

    /**
     * Rate limit for authentication endpoints: 10 requests per minute per IP
     */
    public Bucket resolveAuthBucket(String key) {
        return authBuckets.computeIfAbsent(key, k -> createAuthBucket());
    }

    /**
     * Rate limit for API endpoints: 100 requests per minute per user/IP
     */
    public Bucket resolveApiBucket(String key) {
        return apiBuckets.computeIfAbsent(key, k -> createApiBucket());
    }

    private Bucket createAuthBucket() {
        Bandwidth limit = Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket createApiBucket() {
        Bandwidth limit = Bandwidth.classic(100, Refill.greedy(100, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }
}
