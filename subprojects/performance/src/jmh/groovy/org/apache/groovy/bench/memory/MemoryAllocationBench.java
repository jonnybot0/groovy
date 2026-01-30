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
import java.util.concurrent.TimeUnit;

/**
 * Benchmarks for measuring memory allocation and heap growth in Groovy operations.
 * <p>
 * This benchmark suite quantifies the memory overhead of Groovy's invokedynamic
 * implementation, specifically:
 * <ul>
 *   <li>MethodHandleWrapper with AtomicLong latestHitCount per cached method handle</li>
 *   <li>CacheableCallSite LRU cache with 4 entries per callsite (SoftReference wrapped)</li>
 *   <li>GroovyObjectHelper ClassValue creating AtomicReference per class</li>
 * </ul>
 * <p>
 * Run with: ./gradlew -Pindy=true -PbenchInclude=MemoryAllocation :perf:jmh
 * <p>
 * For detailed allocation profiling:
 * ./gradlew -Pindy=true -PbenchInclude=MemoryAllocation :perf:jmh -Pjmh.profilers=gc
 * <p>
 * Compare indy vs non-indy:
 * ./gradlew -Pindy=false -PbenchInclude=MemoryAllocation :perf:jmh
 * ./gradlew -Pindy=true -PbenchInclude=MemoryAllocation :perf:jmh
 */
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2, jvmArgs = {"-Xms256m", "-Xmx256m"})
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class MemoryAllocationBench {

    private static final int OBJECT_COUNT = 1000;
    private static final int RECEIVER_COUNT = 100;

    // ========================================================================
    // State classes
    // ========================================================================

    @State(Scope.Thread)
    public static class AllocationState {
        List<MemoryReceiver> mixedReceivers;
        List<MemoryReceiver> monomorphicReceivers;

        @Setup(Level.Trial)
        public void setup() {
            mixedReceivers = MemoryHelper.createMixedReceivers(RECEIVER_COUNT);
            monomorphicReceivers = new ArrayList<>(RECEIVER_COUNT);
            for (int i = 0; i < RECEIVER_COUNT; i++) {
                monomorphicReceivers.add(new MemoryReceiverA());
            }
        }
    }

    @State(Scope.Thread)
    public static class FreshObjectState {
        // Objects created fresh per invocation to simulate cold callsites

        @Setup(Level.Invocation)
        public void setup() {
            // Force GC to get consistent memory measurements
            // Note: This is a hint, not a guarantee
            System.gc();
        }
    }

    // ========================================================================
    // Create and call benchmarks - measures cold callsite overhead
    // ========================================================================

    /**
     * Create N objects and call one method on each.
     * This is the most common web application pattern: object created per request,
     * method called once or twice, then object discarded.
     * <p>
     * Each object creation exercises callsite bootstrap in indy mode.
     */
    @Benchmark
    public void memory_createAndCallOnce(FreshObjectState freshState, Blackhole bh) {
        MemoryHelper.createAndCallMany(OBJECT_COUNT, bh);
    }

    /**
     * Create N objects and call multiple methods on each.
     * Tests the memory cost of filling the 4-entry LRU cache per callsite.
     */
    @Benchmark
    public void memory_createAndCallMultiple(FreshObjectState freshState, Blackhole bh) {
        MemoryHelper.createAndCallMultiple(OBJECT_COUNT, bh);
    }

    /**
     * Polymorphic dispatch to mixed types.
     * Tests memory overhead when cache entries are invalidated due to type changes.
     */
    @Benchmark
    public void memory_polymorphicDispatch(AllocationState state, Blackhole bh) {
        MemoryHelper.polymorphicDispatch(state.mixedReceivers, bh);
    }

    /**
     * Monomorphic dispatch (same type).
     * Baseline for comparison - cache should stabilize.
     */
    @Benchmark
    public void memory_monomorphicDispatch(AllocationState state, Blackhole bh) {
        MemoryHelper.polymorphicDispatch(state.monomorphicReceivers, bh);
    }

    // ========================================================================
    // Spread operator benchmarks - tests operator-specific allocation
    // ========================================================================

    /**
     * Spread operator on mixed types.
     * The spread operator creates intermediate lists and exercises callsites.
     */
    @Benchmark
    public void memory_spreadOperatorMixed(AllocationState state, Blackhole bh) {
        MemoryHelper.spreadOperatorOverhead(state.mixedReceivers, bh);
    }

    /**
     * Spread operator on monomorphic types.
     */
    @Benchmark
    public void memory_spreadOperatorMono(AllocationState state, Blackhole bh) {
        MemoryHelper.spreadOperatorOverhead(state.monomorphicReceivers, bh);
    }

    // ========================================================================
    // Closure benchmarks - tests closure allocation overhead
    // ========================================================================

    /**
     * Closure operations on mixed types.
     * Closures create additional objects and capture scope.
     */
    @Benchmark
    public void memory_closureMixed(AllocationState state, Blackhole bh) {
        MemoryHelper.closureOverhead(state.mixedReceivers, bh);
    }

    /**
     * Closure operations on monomorphic types.
     */
    @Benchmark
    public void memory_closureMono(AllocationState state, Blackhole bh) {
        MemoryHelper.closureOverhead(state.monomorphicReceivers, bh);
    }

    // ========================================================================
    // Java baseline benchmarks
    // ========================================================================

    /**
     * Java baseline: create and call.
     * Direct invokevirtual bytecode, no method handle overhead.
     */
    @Benchmark
    public void java_createAndCallOnce(FreshObjectState freshState, Blackhole bh) {
        for (int i = 0; i < OBJECT_COUNT; i++) {
            JavaMemoryService svc = new JavaMemoryService();
            bh.consume(svc.getName());
        }
    }

    /**
     * Java baseline: create and call multiple methods.
     */
    @Benchmark
    public void java_createAndCallMultiple(FreshObjectState freshState, Blackhole bh) {
        for (int i = 0; i < OBJECT_COUNT; i++) {
            JavaMemoryService svc = new JavaMemoryService();
            bh.consume(svc.getName());
            bh.consume(svc.getCounter());
            svc.increment();
            bh.consume(svc.compute(i, i + 1));
        }
    }

    /**
     * Java baseline: polymorphic interface dispatch.
     */
    @Benchmark
    public void java_polymorphicDispatch(AllocationState state, Blackhole bh) {
        for (MemoryReceiver r : state.mixedReceivers) {
            bh.consume(r.receive());
            bh.consume(r.getValue());
        }
    }

    /**
     * Java baseline: monomorphic dispatch.
     */
    @Benchmark
    public void java_monomorphicDispatch(AllocationState state, Blackhole bh) {
        for (MemoryReceiver r : state.monomorphicReceivers) {
            bh.consume(r.receive());
            bh.consume(r.getValue());
        }
    }

    // ========================================================================
    // Java equivalent service class
    // ========================================================================

    public static class JavaMemoryService {
        private String name = "TestService";
        private int counter = 0;

        public String getName() { return name; }
        public int getCounter() { return counter; }
        public void increment() { counter++; }
        public int compute(int a, int b) { return a + b; }
    }
}
