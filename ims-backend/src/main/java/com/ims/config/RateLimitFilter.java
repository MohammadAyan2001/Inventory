package com.ims.config;

import com.ims.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IP-based rate limiting using Bucket4j token bucket algorithm.
 *
 * Each IP gets its own bucket with a fixed capacity.
 * Tokens refill at a steady rate — bursts are allowed up to capacity.
 *
 * In production, use Redis-backed Bucket4j for distributed rate limiting
 * across multiple app instances.
 */
@Component
public class RateLimitFilter implements Filter {

    @Value("${app.rate-limit.capacity:100}")
    private int capacity;

    @Value("${app.rate-limit.refill-tokens:100}")
    private int refillTokens;

    @Value("${app.rate-limit.refill-duration:60}")
    private int refillDuration;

    // ConcurrentHashMap is thread-safe for per-IP bucket storage
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        String ip = ((HttpServletRequest) request).getRemoteAddr();
        Bucket bucket = buckets.computeIfAbsent(ip, this::createBucket);

        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException();
        }
        chain.doFilter(request, response);
    }

    private Bucket createBucket(String ip) {
        Bandwidth limit = Bandwidth.classic(capacity,
            Refill.greedy(refillTokens, Duration.ofSeconds(refillDuration)));
        return Bucket.builder().addLimit(limit).build();
    }
}
