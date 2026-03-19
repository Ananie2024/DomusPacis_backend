package com.domuspacis.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

@Aspect
@Component
@Order(3)
@Slf4j
public class TransactionLoggingAspect {

    @Around("com.domuspacis.aop.PointcutLibrary.transactionalFinanceMethods()")
    public Object logFinancialTransaction(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature sig  = (MethodSignature) joinPoint.getSignature();
        Method          method = sig.getMethod();

        // Skip read-only transactions
        Transactional tx = method.getAnnotation(Transactional.class);
        if (tx != null && tx.readOnly()) {
            return joinPoint.proceed();
        }

        String className  = sig.getDeclaringType().getSimpleName();
        String methodName = sig.getName();
        String correlationId = MDC.get("correlationId");
        String user = extractUser();
        long   start = System.currentTimeMillis();

        log.info("TX START  | correlationId={} method={}.{} user={}",
                correlationId, className, methodName, user);

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;
            log.info("TX COMMIT | correlationId={} method={}.{} durationMs={} user={}",
                    correlationId, className, methodName, duration, user);
            return result;

        } catch (ObjectOptimisticLockingFailureException ex) {
            long duration = System.currentTimeMillis() - start;
            log.warn("TX LOCK CONFLICT | correlationId={} method={}.{} durationMs={} entity={} message={}",
                    correlationId, className, methodName, duration,
                    ex.getPersistentClassName(), ex.getMessage());
            throw ex;

        } catch (Throwable ex) {
            long duration = System.currentTimeMillis() - start;
            log.error("TX ROLLBACK | correlationId={} method={}.{} durationMs={} reason={} message={}",
                    correlationId, className, methodName, duration,
                    ex.getClass().getSimpleName(), ex.getMessage());
            throw ex;
        }
    }

    private String extractUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "system";
    }
}
