package com.domuspacis.aop;

import com.domuspacis.aop.annotation.SensitiveParam;
import com.domuspacis.aop.annotation.SkipLogging;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.time.Instant;

@Aspect
@Component
@Order(4)
@Slf4j
@RequiredArgsConstructor
public class LoggingAspect {

    private final MeterRegistry meterRegistry;

    @Value("${aop.performance.warn-threshold-ms:500}")
    private long warnThresholdMs;

    @Around("com.domuspacis.aop.PointcutLibrary.allServiceMethods() && " +
            "!@annotation(com.domuspacis.aop.annotation.SkipLogging)")
    public Object logServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        String module = extractModule(signature.getDeclaringType().getPackageName());
        String user = extractAuthenticatedUser();
        String correlationId = MDC.get("correlationId");

        long start = System.currentTimeMillis();

        log.debug("ENTER | correlationId={} module={} class={} method={} user={} args={}",
                correlationId, module, className, methodName,
                user, maskSensitiveArgs(joinPoint));

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;

            if (duration >= warnThresholdMs) {
                log.warn("SLOW  | correlationId={} module={} class={} method={} durationMs={} user={} slowCall=true",
                        correlationId, module, className, methodName, duration, user);
            } else {
                log.debug("EXIT  | correlationId={} module={} class={} method={} durationMs={} outcome=SUCCESS",
                        correlationId, module, className, methodName, duration);
            }
            return result;
        } catch (Throwable ex) {
            long duration = System.currentTimeMillis() - start;
            log.error("ERROR | correlationId={} module={} class={} method={} durationMs={} outcome=FAILURE exception={} message={}",
                    correlationId, module, className, methodName, duration,
                    ex.getClass().getSimpleName(), ex.getMessage());
            throw ex;
        }
    }

    private String extractModule(String packageName) {
        String[] parts = packageName.split("\\.");
        return parts.length > 2 ? parts[2] : "unknown";
    }

    private String extractAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "anonymous";
    }

    private String maskSensitiveArgs(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Annotation[][] paramAnnotations = method.getParameterAnnotations();
        Object[] args = joinPoint.getArgs();
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < args.length; i++) {
            boolean sensitive = false;
            for (Annotation ann : paramAnnotations[i]) {
                if (ann instanceof SensitiveParam) {
                    sensitive = true;
                    break;
                }
            }
            sb.append(sensitive ? "[REDACTED]" : args[i]);
            if (i < args.length - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }
}
