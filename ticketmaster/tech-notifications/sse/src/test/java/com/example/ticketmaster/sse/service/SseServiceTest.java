package com.example.ticketmaster.sse.service;

import com.example.ticketmaster.sse.model.NotificationEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SseServiceTest {

    private final DeterministicScheduler scheduler = new DeterministicScheduler();
    private final TestSseEmitterFactory emitterFactory = new TestSseEmitterFactory();
    private final SseService service = new SseService(emitterFactory, scheduler);

    @AfterEach
    void tearDown() {
        service.shutdown();
    }

    @Test
    void testSubscribeCreatesEmitter() {
        String userId = "user1";
        
        SseEmitter emitter = service.subscribe(userId);
        
        assertNotNull(emitter);
    }

    private static final class TestSseEmitterFactory implements SseEmitterFactory {

        private final Queue<RecordingSseEmitter> created = new ArrayDeque<>();

        @Override
        public SseEmitter create(long timeoutMillis) {
            RecordingSseEmitter emitter = new RecordingSseEmitter(timeoutMillis);
            created.add(emitter);
            return emitter;
        }

        RecordingSseEmitter takeLastCreated() {
            RecordingSseEmitter emitter = created.poll();
            if (emitter == null) {
                throw new IllegalStateException("No emitter was created");
            }
            return emitter;
        }
    }

    /**
     * A deterministic emitter for unit tests.
     *
     * Real {@link SseEmitter} completion callbacks are tightly coupled to the
     * servlet async lifecycle. In unit tests (no servlet response), relying on
     * those callbacks is flaky.
     */
    private static final class RecordingSseEmitter extends SseEmitter {

        private final List<NotificationEvent> events = new ArrayList<>();

        RecordingSseEmitter(long timeoutMillis) {
            super(timeoutMillis);
        }

        @Override
        public void send(Object object) throws IOException {
            if (!(object instanceof NotificationEvent event)) {
                throw new IllegalArgumentException("Expected NotificationEvent but got: " + object);
            }
            events.add(event);
        }

        List<NotificationEvent> events() {
            return List.copyOf(events);
        }

        NotificationEvent lastEvent() {
            if (events.isEmpty()) {
                throw new IllegalStateException("No events sent");
            }
            return events.get(events.size() - 1);
        }
    }

    private static final class DeterministicScheduler implements Scheduler {

        private long nowNanos;
        private final PriorityQueue<ScheduledTask> tasks = new PriorityQueue<>(Comparator.comparingLong(t -> t.runAtNanos));

        @Override
        public void schedule(Runnable task, long delay, TimeUnit unit) {
            tasks.add(new ScheduledTask(nowNanos + unit.toNanos(delay), task));
        }

        void advance(long amount, TimeUnit unit) {
            nowNanos += unit.toNanos(amount);
            runDueTasks();
        }

        private void runDueTasks() {
            while (!tasks.isEmpty() && tasks.peek().runAtNanos <= nowNanos) {
                tasks.poll().task.run();
            }
        }

        @Override
        public void shutdown() {
            tasks.clear();
        }

        private record ScheduledTask(long runAtNanos, Runnable task) {
        }
    }

    @Test
    void testStartProcessInitializesWithNotReady() throws Exception {
        String userId = "user2";
        
        service.subscribe(userId);
        RecordingSseEmitter emitter = emitterFactory.takeLastCreated();

        service.startProcess(userId);

        assertEquals("NOT_READY", service.getCurrentStatus(userId));
        assertEquals("NOT_READY", emitter.lastEvent().status());
    }

    @Test
    void testStateTransitionsOccurAutomatically() throws Exception {
        String userId = "user3";

        // Subscribe first (typical client flow)
        service.subscribe(userId);
        RecordingSseEmitter emitter = emitterFactory.takeLastCreated();

        // Start process - state transitions run on scheduler
        service.startProcess(userId);

        assertEquals("NOT_READY", service.getCurrentStatus(userId));
        assertEquals(List.of("NOT_READY"), emitter.events().stream().map(NotificationEvent::status).toList());

        scheduler.advance(2, TimeUnit.SECONDS);
        assertEquals("WAITING", service.getCurrentStatus(userId));
        assertEquals(List.of("NOT_READY", "WAITING"), emitter.events().stream().map(NotificationEvent::status).toList());

        scheduler.advance(3, TimeUnit.SECONDS);
        assertEquals("READY", service.getCurrentStatus(userId));
        assertEquals(List.of("NOT_READY", "WAITING", "READY"), emitter.events().stream().map(NotificationEvent::status).toList());
    }

    @Test
    void testMultipleUsersCanSubscribeIndependently() {
        String user1 = "alice";
        String user2 = "bob";
        
        service.subscribe(user1);
        RecordingSseEmitter emitter1 = emitterFactory.takeLastCreated();
        service.subscribe(user2);
        RecordingSseEmitter emitter2 = emitterFactory.takeLastCreated();
        
        assertNotNull(emitter1);
        assertNotNull(emitter2);
        assertNotSame(emitter1, emitter2);
        
        service.startProcess(user1);
        service.startProcess(user2);
        
        scheduler.advance(2, TimeUnit.SECONDS);
        assertEquals("WAITING", service.getCurrentStatus(user1));
        assertEquals("WAITING", service.getCurrentStatus(user2));
        assertEquals(List.of("NOT_READY", "WAITING"), emitter1.events().stream().map(NotificationEvent::status).toList());
        assertEquals(List.of("NOT_READY", "WAITING"), emitter2.events().stream().map(NotificationEvent::status).toList());

        scheduler.advance(3, TimeUnit.SECONDS);
        assertEquals("READY", service.getCurrentStatus(user1));
        assertEquals("READY", service.getCurrentStatus(user2));
        assertEquals(List.of("NOT_READY", "WAITING", "READY"), emitter1.events().stream().map(NotificationEvent::status).toList());
        assertEquals(List.of("NOT_READY", "WAITING", "READY"), emitter2.events().stream().map(NotificationEvent::status).toList());
    }

    @Test
    void testSubscribeAfterProcessStartedSendsCurrentStatus() throws Exception {
        String userId = "user4";
        
        // Start process first
        service.startProcess(userId);
        
        // Wait for WAITING state
        scheduler.advance(2, TimeUnit.SECONDS);
        assertEquals("WAITING", service.getCurrentStatus(userId));
        
        // Now subscribe - should receive current WAITING status
        service.subscribe(userId);
        RecordingSseEmitter emitter = emitterFactory.takeLastCreated();

        assertEquals("WAITING", emitter.lastEvent().status());

        scheduler.advance(3, TimeUnit.SECONDS);
        assertEquals(List.of("WAITING", "READY"), emitter.events().stream().map(NotificationEvent::status).toList());
    }
}
