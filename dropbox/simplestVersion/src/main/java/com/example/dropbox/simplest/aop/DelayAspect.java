package com.example.dropbox.simplest.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Aspect
@Component
public class DelayAspect {

    private static final Logger log = LoggerFactory.getLogger(DelayAspect.class);

    @Around("execution(* com.example.dropbox.simplest.service.FileService.uploadFile(..))")
    public Object addUploadLatency(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        long fileSize = 0;

        for (Object arg : args) {
            if (arg instanceof MultipartFile) {
                fileSize = ((MultipartFile) arg).getSize();
                break;
            }
        }

        // Formula: 5 * file-size * 10 ms
        // For a 100 byte file: 5 * 100 * 10 = 5000ms = 5s
        long delay = 5 * fileSize * 10;

        log.info("Simulating upload latency of {}ms for file size {} bytes", delay, fileSize);

        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return joinPoint.proceed();
    }
}
