package com.example.ticketmaster.sse.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Factory abstraction to create {@link SseEmitter} instances.
 *
 * This keeps production code simple while making unit tests deterministic
 * (unit tests do not run with a real servlet response, so sending SSE data
 * can behave differently).
 */
public interface SseEmitterFactory {

    SseEmitter create(long timeoutMillis);
}
