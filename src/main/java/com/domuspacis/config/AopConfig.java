package com.domuspacis.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Enables AspectJ auto-proxy with CGLIB subclass-based proxying (proxyTargetClass=true).
 * All aspect beans are registered via @Component on each aspect class.
 * Execution order is enforced via @Order on each aspect class.
 * Performance thresholds are injected via @Value directly in PerformanceAspect and LoggingAspect.
 */
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class AopConfig {
    // Aspect beans are self-registering via @Component + @Aspect on each class.
    // @Order on each aspect class controls execution sequence per Section 11.10 of the blueprint.
}
