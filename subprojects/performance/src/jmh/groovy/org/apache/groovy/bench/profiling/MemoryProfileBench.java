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
package org.apache.groovy.bench.profiling;

import org.apache.groovy.bench.memory.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Benchmarks with detailed memory profiling.
 * <p>
 * These benchmarks use MemoryTrackingState to capture detailed memory
 * metrics before and after execution, helping identify memory-intensive
 * operations in Groovy's invokedynamic implementation.
 * <p>
 * Run with: ./gradlew -Pindy=true -PbenchInclude=MemoryProfile :perf:jmh
 * <p>
 * Or with the profiled runner:
 * ./gradlew :perf:jmhJar && \
 * java -cp build/libs/performance-*-jmh.jar \
 *   org.apache.groovy.bench.profiling.ProfiledBenchmarkRunner "MemoryProfile"
 */
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class MemoryProfileBench {

    private static final int BATCH_SIZE = 1000;

    // ========================================================================
    // State classes
    // ========================================================================

    @State(Scope.Thread)
    public static class ProfileState {
        List<MemoryReceiver> receivers;
        Random random;

        @Setup(Level.Trial)
        public void setup() {
            random = new Random(42);
            receivers = MemoryHelper.createMixedReceivers(100);
        }
    }

    // ========================================================================
    // Memory Profile Benchmarks
    // ========================================================================

    /**
     * Profile memory during batch object creation and method calls.
     * This measures callsite allocation overhead.
     */
    @Benchmark
    public void memprofile_batchCreateAndCall(ProfileState state, MemoryTrackingState memState, Blackhole bh) {
        MemoryHelper.createAndCallMany(BATCH_SIZE, bh);
    }

    /**
     * Profile memory during polymorphic dispatch.
     * This measures cache churn overhead.
     */
    @Benchmark
    public void memprofile_polymorphicDispatch(ProfileState state, MemoryTrackingState memState, Blackhole bh) {
        for (int i = 0; i < 100; i++) {
            MemoryHelper.polymorphicDispatch(state.receivers, bh);
        }
    }

    /**
     * Profile memory during closure operations.
     */
    @Benchmark
    public void memprofile_closureOperations(ProfileState state, MemoryTrackingState memState, Blackhole bh) {
        for (int i = 0; i < 100; i++) {
            MemoryHelper.closureOverhead(state.receivers, bh);
        }
    }

    /**
     * Profile memory during spread operator use.
     */
    @Benchmark
    public void memprofile_spreadOperator(ProfileState state, MemoryTrackingState memState, Blackhole bh) {
        for (int i = 0; i < 100; i++) {
            MemoryHelper.spreadOperatorOverhead(state.receivers, bh);
        }
    }

    /**
     * Profile memory during session simulation.
     * This measures real-world web app memory patterns.
     */
    @Benchmark
    public void memprofile_sessionSimulation(ProfileState state, MemoryTrackingState memState, Blackhole bh) {
        for (int i = 0; i < BATCH_SIZE; i++) {
            MemoryHelper.simulateRequest("request-" + i, bh);
        }
    }

    /**
     * Heavy benchmark to stress memory allocation.
     * Creates many objects and performs many method calls.
     */
    @Benchmark
    public void memprofile_heavyLoad(ProfileState state, MemoryTrackingState memState, Blackhole bh) {
        // Phase 1: Object creation
        for (int i = 0; i < BATCH_SIZE; i++) {
            MemoryTestService svc = new MemoryTestService();
            bh.consume(svc.getName());
            bh.consume(svc.compute(i, i + 1));
        }

        // Phase 2: Polymorphic dispatch
        for (int round = 0; round < 10; round++) {
            MemoryHelper.polymorphicDispatch(state.receivers, bh);
        }

        // Phase 3: Collection operations
        for (int round = 0; round < 10; round++) {
            MemoryHelper.closureOverhead(state.receivers, bh);
        }

        // Phase 4: Session simulation
        for (int i = 0; i < 100; i++) {
            MemoryHelper.simulateRequest("heavy-" + i, bh);
        }
    }

    /**
     * Baseline: Java equivalent operations for comparison.
     */
    @Benchmark
    public void memprofile_javaBaseline(ProfileState state, MemoryTrackingState memState, Blackhole bh) {
        // Object creation
        for (int i = 0; i < BATCH_SIZE; i++) {
            JavaTestService svc = new JavaTestService();
            bh.consume(svc.getName());
            bh.consume(svc.compute(i, i + 1));
        }

        // Interface dispatch
        for (int round = 0; round < 10; round++) {
            for (MemoryReceiver r : state.receivers) {
                bh.consume(r.receive());
                bh.consume(r.getValue());
            }
        }
    }

    // Java baseline class
    public static class JavaTestService {
        private String name = "JavaTestService";
        private int counter = 0;

        public String getName() { return name; }
        public int getCounter() { return counter; }
        public int compute(int a, int b) { return a + b; }
    }
}
