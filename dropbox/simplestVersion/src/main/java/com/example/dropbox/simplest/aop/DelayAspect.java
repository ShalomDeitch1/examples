package com.example.dropbox.simplest.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Random;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class DelayAspect {

    private static final Logger log = LoggerFactory.getLogger(DelayAspect.class);
    private final Random random = new Random();

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface SimulateLatency {
        long minMs() default 100;
        long maxMs() default 500;
    }

    @Around("@annotation(latency)")
    public Object addLatency(ProceedingJoinPoint joinPoint, SimulateLatency latency) throws Throwable {
        long delay = random.nextLong(latency.minMs(), latency.maxMs());
        log.info("Simulating latency of {}ms for {}", delay, joinPoint.getSignature().getName());
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return joinPoint.proceed();
    }
}
