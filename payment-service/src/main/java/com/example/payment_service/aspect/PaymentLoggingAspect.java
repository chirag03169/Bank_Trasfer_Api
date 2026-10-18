package com.example.payment_service.aspect;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class PaymentLoggingAspect {

    @Around("execution(* com.example.payment_service.serviceImpl..*(..))")
    public Object logServiceExecution(ProceedingJoinPoint joinPoint)
            throws Throwable {

        String methodName = joinPoint.getSignature().toShortString();
        long startTime = System.currentTimeMillis();

        log.info("Starting service method: {}", methodName);

        try {
            Object result = joinPoint.proceed();

            long duration = System.currentTimeMillis() - startTime;

            log.info(
                    "Completed service method: {} in {} ms",
                    methodName,
                    duration
            );

            return result;

        } catch (Throwable exception) {
            log.error(
                    "Service method failed: {}",
                    methodName,
                    exception
            );

            throw exception;
        }
    }
}

