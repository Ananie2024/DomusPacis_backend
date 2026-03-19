package com.domuspacis.aop;

import com.domuspacis.auth.domain.SecurityAuditLog;
import com.domuspacis.auth.infrastructure.SecurityAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;

@Aspect
@Component
@Order(1)
@Slf4j
@RequiredArgsConstructor
public class SecurityAuditAspect {

    private final SecurityAuditLogRepository securityAuditLogRepository;

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAuthSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        log.info("AUTH_SUCCESS | user={}", username);
        persistSecurityLog("AUTH_SUCCESS", username, "SUCCESS",
                "Successful login at " + Instant.now());
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAuthFailure(AbstractAuthenticationFailureEvent event) {
        String username = event.getAuthentication().getName();
        String reason = event.getException().getMessage();
        log.warn("AUTH_FAILURE | user={} reason={}", username, reason);
        persistSecurityLog("AUTH_FAILURE", username, "FAILURE", "Reason: " + reason);
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAccessDenied(AuthorizationDeniedEvent<?> event) {
        String user = event.getAuthentication().get() != null
                ? event.getAuthentication().get().getName() : "anonymous";
        log.warn("ACCESS_DENIED | user={}", user);
        persistSecurityLog("ACCESS_DENIED", user, "FAILURE", "Authorization denied");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistSecurityLog(String eventType, String username, String outcome, String detail) {
        String ip = extractIp();
        String ua = extractUserAgent();
        SecurityAuditLog entry = SecurityAuditLog.builder()
                .eventType(eventType)
                .username(username)
                .ipAddress(ip)
                .userAgent(ua)
                .outcome(outcome)
                .detail(detail)
                .timestamp(Instant.now())
                .build();
        securityAuditLogRepository.save(entry);
    }

    private String extractIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs != null ? attrs.getRequest().getRemoteAddr() : "unknown";
        } catch (Exception e) { return "unknown"; }
    }

    private String extractUserAgent() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs != null ? attrs.getRequest().getHeader("User-Agent") : "unknown";
        } catch (Exception e) { return "unknown"; }
    }
}
