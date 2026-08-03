package com.domuspacis.auth.application;

import com.domuspacis.auth.domain.User;
import com.domuspacis.auth.infrastructure.UserRepository;
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
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filter that applies brute-force protection to the login endpoint.
 *
 * Two layers of defence:
 * 1. Per-IP rate limiting via Bucket4j (in-memory token bucket).
 * 2. Per-account lockout after N consecutive failed attempts (persisted in DB).
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final ClientIpResolver clientIpResolver;

    @Value("${app.rate-limit.login.max-requests-per-minute:5}")
    private int maxRequestsPerMinute;

    @Value("${app.rate-limit.login.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${app.rate-limit.login.lockout-duration-minutes:15}")
    private int lockoutDurationMinutes;

    /** Per-IP token buckets. */
    private final Map<String, Bucket> ipBuckets = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !("/api/v1/auth/login".equals(path) && "POST".equalsIgnoreCase(request.getMethod()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // ── Layer 1: IP-based rate limiting ──────────────────────────────
        String clientIp = clientIpResolver.resolveClientIp(request);
        Bucket bucket = ipBuckets.computeIfAbsent(clientIp, this::newBucket);

        if (!bucket.tryConsume(1)) {
            log.warn("Rate limit exceeded for IP: {}", clientIp);
            writeError(response, 429,
                    "Too many login attempts. Please try again later.");
            return;
        }

        // ── Layer 2: Account lockout check ───────────────────────────────
        // We need to read the email from the request body before the auth filter
        // consumes the stream. We'll wrap the request to make the body reusable.
        CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(request);
        String email = extractEmail(wrappedRequest);

        if (email != null) {
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (isAccountLocked(user)) {
                    log.warn("Locked account login attempt for email: {}", email);
                    writeError(response, 429,
                            "Account is temporarily locked due to too many failed attempts. Try again later.");
                    return;
                }
            }
        }

        // ── Proceed with the filter chain ────────────────────────────────
        // Use a custom response wrapper to capture the status code so we can
        // detect whether authentication succeeded or failed.
        StatusCapturingResponseWrapper responseWrapper =
                new StatusCapturingResponseWrapper(response);

        filterChain.doFilter(wrappedRequest, responseWrapper);

        // ── Post-authentication: track success/failure ───────────────────
        // This is best-effort bookkeeping.  If the DB write fails we must NOT
        // let it corrupt the already-written response (e.g. turn a 401 into a
        // 500), so every failure is caught and logged.
        if (email != null) {
            try {
                int status = responseWrapper.getStatus();
                if (status == HttpServletResponse.SC_OK) {
                    // Successful login → reset failed attempts
                    userRepository.findByEmail(email).ifPresent(u -> {
                        if (u.getFailedLoginAttempts() > 0 || u.getLockedUntil() != null) {
                            u.setFailedLoginAttempts(0);
                            u.setLockedUntil(null);
                            userRepository.save(u);
                            log.info("Login succeeded – reset lockout state for: {}", email);
                        }
                    });
                } else if (status == HttpServletResponse.SC_UNAUTHORIZED) {
                    // Failed login → increment failed attempts
                    userRepository.findByEmail(email).ifPresent(u -> {
                        int attempts = u.getFailedLoginAttempts() + 1;
                        u.setFailedLoginAttempts(attempts);
                        if (attempts >= maxFailedAttempts) {
                            u.setLockedUntil(LocalDateTime.now().plusMinutes(lockoutDurationMinutes));
                            log.warn("Account locked after {} failed attempts: {}", attempts, email);
                        }
                        userRepository.save(u);
                    });
                }
            } catch (Exception e) {
                // Never let lockout bookkeeping break the login response.
                log.error("Failed to update login attempt tracking for {}: {}", email, e.getMessage());
            }
        }
    }

    private boolean isAccountLocked(User user) {
        if (user.getLockedUntil() == null) {
            return false;
        }
        if (LocalDateTime.now().isBefore(user.getLockedUntil())) {
            return true;
        }
        // Lockout period has expired – clear it
        user.setLockedUntil(null);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);
        return false;
    }

    private String extractEmail(HttpServletRequest request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = objectMapper.readValue(
                    request.getInputStream(), Map.class);
            Object email = body.get("email");
            return email != null ? email.toString() : null;
        } catch (Exception e) {
            return null;
        }
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