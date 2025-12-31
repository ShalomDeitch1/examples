package com.example.ticketmaster.sse.service;

import java.util.concurrent.TimeUnit;

interface Scheduler {

    void schedule(Runnable task, long delay, TimeUnit unit);

    void shutdown();
}
