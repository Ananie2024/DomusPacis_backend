package com.domuspacis.aop;

import com.domuspacis.aop.annotation.Audited;
import com.domuspacis.auth.domain.AuditLog;
import com.domuspacis.auth.infrastructure.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.UUID;

@Aspect
@Component
@Order(2)
@Slf4j
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;

    @Around("@annotation(audited)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String actionLabel = audited.value().isBlank()
                ? signature.getName().toUpperCase()
                : audited.value();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID actorId = null;
        String actorName = "system";
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            actorName = auth.getName();
        }

        String ipAddress = extractIpAddress();
        String userAgent = extractUserAgent();
        String entityType = extractEntityType(joinPoint);

        try {
            Object result = joinPoint.proceed();
            UUID entityId = extractEntityId(result);
            persistAuditLog(actorId, actorName, actionLabel, entityType, entityId,
                    AuditLog.AuditOutcome.SUCCESS, null, ipAddress, userAgent);
            return result;
        } catch (Throwable ex) {
            persistAuditLog(actorId, actorName, actionLabel, entityType, null,
                    AuditLog.AuditOutcome.FAILURE,
                    truncate(ex.getMessage(), 500),
                    ipAddress, userAgent);
            throw ex;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistAuditLog(UUID actorId, String actorName, String actionLabel,
                                 String entityType, UUID entityId, AuditLog.AuditOutcome outcome,
                                 String failureReason, String ipAddress, String userAgent) {
        AuditLog entry = AuditLog.builder()
                .actorId(actorId)
                .actorName(actorName)
                .actionLabel(actionLabel)
                .entityType(entityType)
                .entityId(entityId)
                .outcome(outcome)
                .failureReason(failureReason)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
        auditLogRepository.save(entry);
    }

    private String extractEntityType(ProceedingJoinPoint joinPoint) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        return className.replace("Service", "").replace("Impl", "");
    }

    private UUID extractEntityId(Object result) {
        if (result == null) return null;
        try {
            Method getId = result.getClass().getMethod("id");
            Object id = getId.invoke(result);
            if (id instanceof UUID uuid) return uuid;
        } catch (Exception ignored) {}
        return null;
    }

    private String extractIpAddress() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) return attrs.getRequest().getRemoteAddr();
        } catch (Exception ignored) {}
        return "unknown";
    }

    private String extractUserAgent() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) return attrs.getRequest().getHeader("User-Agent");
        } catch (Exception ignored) {}
        return "unknown";
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() > maxLen ? s.substring(0, maxLen) : s;
    }
}
