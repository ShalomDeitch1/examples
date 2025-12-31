package com.example.ticketmaster.sse.service;

import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

final class ExecutorScheduler implements Scheduler {

    private final ScheduledExecutorService delegate;

    ExecutorScheduler(ScheduledExecutorService delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public void schedule(Runnable task, long delay, TimeUnit unit) {
        delegate.schedule(task, delay, unit);
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }
}
