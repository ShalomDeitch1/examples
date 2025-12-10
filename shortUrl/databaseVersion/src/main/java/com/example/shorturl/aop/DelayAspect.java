package com.example.shorturl.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Aspect
@Component
public class DelayAspect {

    private final DelayManager delayManager;

    public DelayAspect(DelayManager delayManager) {
        this.delayManager = delayManager;
    }

    // Target only findById in ShortUrlRepository
    @Around("execution(* com.example.shorturl.repository.ShortUrlRepository.findById(..))")
    public Object addDelay(ProceedingJoinPoint joinPoint) throws Throwable {
        Duration delay = delayManager.getRandomDelay();
        if (!delay.isZero()) {
            Thread.sleep(delay.toMillis());
        }
        return joinPoint.proceed();
    }
}
