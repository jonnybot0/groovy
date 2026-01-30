/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.groovy.bench.memory;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Benchmarks simulating web application memory patterns over time.
 * <p>
 * Web applications create many short-lived objects per request:
 * <ul>
 *   <li>Session objects created on request start</li>
 *   <li>Domain objects fetched/created during request processing</li>
 *   <li>All objects discarded at request end</li>
 * </ul>
 * <p>
 * With Groovy 4's invokedynamic:
 * <ul>
 *   <li>Each request exercises callsites that may never be fully optimized</li>
 *   <li>MethodHandleWrapper AtomicLong objects accumulate</li>
 *   <li>ClassValue entries per class type persist</li>
 * </ul>
 * <p>
 * Run with: ./gradlew -Pindy=true -PbenchInclude=LongRunningSession :perf:jmh
 * <p>
 * For memory profiling:
 * ./gradlew -Pindy=true -PbenchInclude=LongRunningSession :perf:jmh -Pjmh.profilers=gc
 */
@Warmup(iterations = 2, time = 3, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2, jvmArgs = {"-Xms512m", "-Xmx512m"})
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class LongRunningSessionBench {

    private static final int REQUESTS_PER_CYCLE = 100;

    // ========================================================================
    // State classes
    // ========================================================================

    @State(Scope.Thread)
    public static class SessionState {
        int requestCounter = 0;
        List<MemoryReceiver> servicePool;

        @Setup(Level.Trial)
        public void setup() {
            // Simulate a pool of service objects (like Spring beans)
            servicePool = MemoryHelper.createMixedReceivers(50);
        }

        @Setup(Level.Iteration)
        public void resetCounter() {
            requestCounter = 0;
        }

        public String nextRequestId() {
            return "req-" + (requestCounter++);
        }
    }

    @State(Scope.Benchmark)
    public static class SustainedLoadState {
        long heapBeforeLoad;
        long heapAfterLoad;

        @Setup(Level.Trial)
        public void captureInitialHeap() {
            System.gc();
            heapBeforeLoad = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        }
    }

    // ========================================================================
    // Request cycle benchmarks
    // ========================================================================

    /**
     * Single request cycle: create session, process, discard.
     * This is the atomic unit of web application work.
     */
    @Benchmark
    public void session_requestCycle(SessionState state, Blackhole bh) {
        // Simulate single request
        String requestId = state.nextRequestId();
        Object result = MemoryHelper.simulateRequest(requestId, bh);
        bh.consume(result);
    }

    /**
     * Request cycle with service calls (more realistic).
     * Simulates controller -> service -> repository pattern.
     */
    @Benchmark
    public void session_requestWithServices(SessionState state, Blackhole bh) {
        String requestId = state.nextRequestId();

        // Controller creates session
        Object sessionResult = MemoryHelper.simulateRequest(requestId, bh);

        // Service layer calls
        List<MemoryReceiver> services = state.servicePool;
        for (int i = 0; i < 5; i++) {
            MemoryReceiver service = services.get(i % services.size());
            bh.consume(service.receive());
            bh.consume(service.getValue());
        }

        bh.consume(sessionResult);
    }

    // ========================================================================
    // Sustained load benchmarks
    // ========================================================================

    /**
     * 1000 request cycles to measure memory accumulation.
     * After 1000 requests, measure final heap to check for memory growth.
     */
    @Benchmark
    public void session_sustainedLoad(SessionState state, SustainedLoadState loadState, Blackhole bh) {
        // Process 1000 requests
        for (int i = 0; i < 1000; i++) {
            String requestId = state.nextRequestId();
            Object result = MemoryHelper.simulateRequest(requestId, bh);

            // Occasional service calls (like real apps)
            if (i % 10 == 0) {
                List<MemoryReceiver> services = state.servicePool;
                for (int j = 0; j < 3; j++) {
                    bh.consume(services.get(j % services.size()).receive());
                }
            }

            bh.consume(result);
        }

        // Capture heap after load
        System.gc();
        loadState.heapAfterLoad = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    /**
     * Heavy sustained load: 5000 requests with polymorphic dispatch.
     */
    @Benchmark
    public void session_heavyLoad(SessionState state, Blackhole bh) {
        List<MemoryReceiver> services = state.servicePool;

        for (int i = 0; i < 5000; i++) {
            // Create fresh session data each request
            String requestId = "heavy-" + i;
            SessionData session = new SessionData();
            session.setSessionId(requestId);
            session.setUserId("user-" + (i % 100));
            session.setCreatedAt(System.currentTimeMillis());
            session.setLastAccessed(System.currentTimeMillis());

            // Process request with polymorphic service calls
            session.setAttribute("iteration", i);
            for (int j = 0; j < 5; j++) {
                MemoryReceiver service = services.get((i + j) % services.size());
                session.setAttribute("result-" + j, service.receive());
            }

            bh.consume(session.getSummary());
        }
    }

    // ========================================================================
    // Memory recovery benchmarks
    // ========================================================================

    /**
     * Test if GC can reclaim memory after load.
     * Runs load, forces GC, measures remaining heap.
     */
    @Benchmark
    public void session_memoryRecovery(SessionState state, Blackhole bh) {
        // Phase 1: Create load
        for (int i = 0; i < 1000; i++) {
            String requestId = state.nextRequestId();
            Object result = MemoryHelper.simulateRequest(requestId, bh);
            bh.consume(result);
        }

        // Phase 2: Let objects become eligible for GC
        // (no references held - sessions should be collectible)

        // Phase 3: Force GC
        System.gc();

        // Phase 4: More requests to see if memory stabilizes
        for (int i = 0; i < 1000; i++) {
            String requestId = state.nextRequestId();
            Object result = MemoryHelper.simulateRequest(requestId, bh);
            bh.consume(result);
        }
    }

    /**
     * Rapid object churn: create and discard objects quickly.
     * Tests if SoftReferences in cache are cleared under memory pressure.
     */
    @Benchmark
    public void session_rapidChurn(SessionState state, Blackhole bh) {
        List<MemoryReceiver> services = state.servicePool;

        for (int round = 0; round < 10; round++) {
            // Create burst of activity
            List<SessionData> sessions = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                SessionData session = new SessionData();
                session.setSessionId(UUID.randomUUID().toString());
                session.setUserId("user-" + i);
                session.setCreatedAt(System.currentTimeMillis());
                session.setLastAccessed(System.currentTimeMillis());

                // Service calls
                for (int j = 0; j < 3; j++) {
                    session.setAttribute("data-" + j, services.get((i + j) % services.size()).receive());
                }

                sessions.add(session);
            }

            // Process all sessions
            for (SessionData session : sessions) {
                bh.consume(session.getSummary());
                bh.consume(session.getAttribute("data-0"));
            }

            // Clear references
            sessions.clear();

            // Small pause between rounds
            if (round < 9) {
                System.gc();
            }
        }
    }

    // ========================================================================
    // Java baseline benchmarks
    // ========================================================================

    /**
     * Java baseline: request cycle without Groovy dispatch.
     */
    @Benchmark
    public void java_requestCycle(SessionState state, Blackhole bh) {
        String requestId = state.nextRequestId();
        JavaSession session = new JavaSession(requestId, "user-" + requestId.hashCode() % 1000);
        session.setAttribute("requestId", requestId);
        session.setAttribute("timestamp", System.currentTimeMillis());
        bh.consume(session.getSummary());
    }

    /**
     * Java baseline: sustained load.
     */
    @Benchmark
    public void java_sustainedLoad(SessionState state, Blackhole bh) {
        for (int i = 0; i < 1000; i++) {
            String requestId = state.nextRequestId();
            JavaSession session = new JavaSession(requestId, "user-" + i % 100);
            session.setAttribute("iteration", i);

            // Service calls through interface
            List<MemoryReceiver> services = state.servicePool;
            if (i % 10 == 0) {
                for (int j = 0; j < 3; j++) {
                    bh.consume(services.get(j % services.size()).receive());
                }
            }

            bh.consume(session.getSummary());
        }
    }

    // ========================================================================
    // Java session equivalent
    // ========================================================================

    public static class JavaSession {
        private final String sessionId;
        private final String userId;
        private final java.util.Map<String, Object> attributes = new java.util.HashMap<>();
        private final long createdAt;
        private long lastAccessed;

        public JavaSession(String sessionId, String userId) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.createdAt = System.currentTimeMillis();
            this.lastAccessed = this.createdAt;
        }

        public void setAttribute(String key, Object value) {
            attributes.put(key, value);
            lastAccessed = System.currentTimeMillis();
        }

        public Object getAttribute(String key) {
            lastAccessed = System.currentTimeMillis();
            return attributes.get(key);
        }

        public String getSummary() {
            return "Session[" + sessionId + "]: user=" + userId + ", attrs=" + attributes.size();
        }
    }
}
