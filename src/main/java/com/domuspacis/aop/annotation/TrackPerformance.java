package com.domuspacis.aop.annotation;
import java.lang.annotation.*;
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TrackPerformance { long warnMs() default 500L; long criticalMs() default 2000L; }
