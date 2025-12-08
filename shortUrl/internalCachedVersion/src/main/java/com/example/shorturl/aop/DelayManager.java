package com.example.shorturl.aop;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class DelayManager {

    private final AtomicInteger minDelayMs = new AtomicInteger(20);
    private final AtomicInteger maxDelayMs = new AtomicInteger(20); // Default to fixed 20ms

    public void setDelayRange(int minMs, int maxMs) {
        if (minMs < 0 || maxMs < 0 || minMs > maxMs) {
            throw new IllegalArgumentException("Invalid delay range: min=" + minMs + ", max=" + maxMs);
        }
        this.minDelayMs.set(minMs);
        this.maxDelayMs.set(maxMs);
    }

    public Duration getRandomDelay() {
        int min = minDelayMs.get();
        int max = maxDelayMs.get();
        if (min == max) {
            return Duration.ofMillis(min);
        }
        return Duration.ofMillis(ThreadLocalRandom.current().nextInt(min, max + 1));
    }

    public int getMinDelay() {
        return minDelayMs.get();
    }

    public int getMaxDelay() {
        return maxDelayMs.get();
    }
}
