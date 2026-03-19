package com.domuspacis.aop;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(6)
@Slf4j
@RequiredArgsConstructor
public class ExceptionLoggingAspect {

    private final MeterRegistry meterRegistry;

    @AfterThrowing(
        pointcut = "com.domuspacis.aop.PointcutLibrary.allServiceMethods() || " +
                   "com.domuspacis.aop.PointcutLibrary.allControllerMethods()",
        throwing = "ex"
    )
    public void logException(JoinPoint joinPoint, Throwable ex) {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        String correlationId = MDC.get("correlationId");
        String method = sig.getDeclaringType().getSimpleName() + "." + sig.getName();

        if (ex instanceof DataIntegrityViolationException || ex instanceof jakarta.validation.ConstraintViolationException) {
            log.error("DATA_INTEGRITY | correlationId={} method={} exception={} message={}",
                    correlationId, method, ex.getClass().getSimpleName(), ex.getMessage());
            meterRegistry.counter("domuspacis.errors", "type", "data_integrity").increment();

        } else if (ex instanceof AccessDeniedException || ex instanceof AuthenticationException) {
            log.warn("SECURITY | correlationId={} method={} exception={}",
                    correlationId, method, ex.getClass().getSimpleName());

        } else if (ex instanceof ObjectOptimisticLockingFailureException) {
            log.warn("OPTIMISTIC_LOCK | correlationId={} method={} message={}",
                    correlationId, method, ex.getMessage());

        } else if (ex instanceof com.domuspacis.shared.exception.ResourceNotFoundException
                || ex instanceof com.domuspacis.shared.exception.BookingConflictException) {
            log.warn("BUSINESS | correlationId={} method={} exception={} message={}",
                    correlationId, method, ex.getClass().getSimpleName(), ex.getMessage());

        } else {
            log.error("UNHANDLED | correlationId={} method={} exception={} message={}",
                    correlationId, method, ex.getClass().getName(), ex.getMessage(), ex);
            meterRegistry.counter("domuspacis.errors", "type", "unhandled").increment();
        }
    }
}
