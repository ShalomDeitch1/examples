package com.example.ticketmaster.sse.service;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class DefaultSseEmitterFactory implements SseEmitterFactory {

    @Override
    public SseEmitter create(long timeoutMillis) {
        return new SseEmitter(timeoutMillis);
    }
}
