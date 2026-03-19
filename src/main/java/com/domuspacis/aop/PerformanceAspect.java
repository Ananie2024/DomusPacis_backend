package com.domuspacis.aop;

import com.domuspacis.aop.annotation.SkipLogging;
import com.domuspacis.aop.annotation.TrackPerformance;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@Order(5)
@Slf4j
@RequiredArgsConstructor
public class PerformanceAspect {

    private final MeterRegistry meterRegistry;

    @Value("${aop.performance.warn-threshold-ms:500}")
    private long defaultWarnMs;

    @Value("${aop.performance.critical-threshold-ms:2000}")
    private long defaultCriticalMs;

    @Around("com.domuspacis.aop.PointcutLibrary.allServiceMethods() && " +
            "!@annotation(com.domuspacis.aop.annotation.SkipLogging)")
    public Object measurePerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        long warnMs = defaultWarnMs;
        long criticalMs = defaultCriticalMs;

        TrackPerformance track = method.getAnnotation(TrackPerformance.class);
        if (track != null) {
            warnMs = track.warnMs();
            criticalMs = track.criticalMs();
        }

        long start = System.currentTimeMillis();
        try {
            return joinPoint.proceed();
        } finally {
            long duration = System.currentTimeMillis() - start;
            String className = signature.getDeclaringType().getSimpleName();
            String methodName = signature.getName();

            if (duration >= criticalMs) {
                log.error("PERF CRITICAL | {}.{} took {}ms (threshold={}ms)",
                        className, methodName, duration, criticalMs);
                meterRegistry.counter("domuspacis.perf.critical",
                        "class", className, "method", methodName).increment();
            } else if (duration >= warnMs) {
                log.warn("PERF WARN | {}.{} took {}ms (threshold={}ms)",
                        className, methodName, duration, warnMs);
                meterRegistry.counter("domuspacis.perf.warn",
                        "class", className, "method", methodName).increment();
            }
        }
    }
}
