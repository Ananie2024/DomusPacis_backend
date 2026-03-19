package com.domuspacis.aop;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PointcutLibrary {

    @Pointcut("execution(* com.domuspacis.*.application.*Service.*(..))")
    public void allServiceMethods() {}

    @Pointcut("execution(* com.domuspacis.*.interfaces.*Controller.*(..))")
    public void allControllerMethods() {}

    @Pointcut("execution(* com.domuspacis.booking.application.*.*(..))")
    public void bookingServiceMethods() {}

    @Pointcut("execution(* com.domuspacis.finance.application.*.*(..))")
    public void financeServiceMethods() {}

    @Pointcut("execution(* com.domuspacis.tax.application.*.*(..))")
    public void taxServiceMethods() {}

    @Pointcut("execution(* com.domuspacis.staff.application.*.*(..))")
    public void staffServiceMethods() {}

    @Pointcut("execution(* com.domuspacis.inventory.application.*.*(..))")
    public void inventoryServiceMethods() {}

    @Pointcut("execution(* com.domuspacis.auth.application.*Service.*(..))")
    public void authServiceMethods() {}

    @Pointcut("@annotation(com.domuspacis.aop.annotation.Audited)")
    public void adminOperations() {}

    @Pointcut("@annotation(org.springframework.transaction.annotation.Transactional) && financeServiceMethods()")
    public void transactionalFinanceMethods() {}

    @Pointcut("authServiceMethods() || taxServiceMethods()")
    public void sensitiveOperations() {}

    @Pointcut("!@annotation(com.domuspacis.aop.annotation.SkipLogging)")
    public void notSkipLogging() {}
}
