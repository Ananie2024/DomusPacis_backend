package com.domuspacis.auth.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filter that applies rate limiting to the password reset initiate endpoint.
 *
 * Prevents abuse/spam by limiting the number of password reset requests
 * from a single IP address.
 */
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class PasswordResetRateLimitFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final ClientIpResolver clientIpResolver;

    @Value("${app.rate-limit.password-reset.max-requests-per-minute:3}")
    private int maxRequestsPerMinute;

    /** Per-IP token buckets. */
    private final Map<String, Bucket> ipBuckets = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !("/api/v1/auth/password-reset/initiate".equals(path) && "POST".equalsIgnoreCase(request.getMethod()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // IP-based rate limiting
        String clientIp = clientIpResolver.resolveClientIp(request);
        Bucket bucket = ipBuckets.computeIfAbsent(clientIp, this::newBucket);

        if (!bucket.tryConsume(1)) {
            log.warn("Password reset rate limit exceeded for IP: {}", clientIp);
            writeError(response, 429,
                    "Too many password reset requests. Please try again later.");
            return;
        }

        // Proceed with the filter chain
        filterChain.doFilter(request, response);
    }


    private Bucket newBucket(String ip) {
        Bandwidth limit = Bandwidth.classic(
                maxRequestsPerMinute,
                Refill.intervally(maxRequestsPerMinute, Duration.ofMinutes(1))
        );
        return Bucket.builder().addLimit(limit).build();
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(),
                Map.of("status", "error", "message", message));
    }
}