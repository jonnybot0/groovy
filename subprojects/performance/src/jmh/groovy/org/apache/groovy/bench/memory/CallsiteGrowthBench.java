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
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Benchmarks for measuring how memory grows as unique callsites accumulate.
 * <p>
 * With Groovy 4's invokedynamic implementation:
 * <ul>
 *   <li>Each callsite creates a CacheableCallSite with 4 cache entries</li>
 *   <li>Each cache entry is wrapped in SoftReference with MethodHandleWrapper</li>
 *   <li>MethodHandleWrapper contains AtomicLong (~32 bytes each)</li>
 * </ul>
 * <p>
 * With millions of callsites × 4 cache entries = significant memory overhead.
 * <p>
 * Run with: ./gradlew -Pindy=true -PbenchInclude=CallsiteGrowth :perf:jmh
 * <p>
 * For GC profiling:
 * ./gradlew -Pindy=true -PbenchInclude=CallsiteGrowth :perf:jmh -Pjmh.profilers=gc
 */
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2, jvmArgs = {"-Xms256m", "-Xmx256m"})
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class CallsiteGrowthBench {

    // ========================================================================
    // Parameterized callsite counts
    // ========================================================================

    @Param({"100", "1000", "10000"})
    public int callsiteCount;

    // ========================================================================
    // State classes
    // ========================================================================

    @State(Scope.Benchmark)
    public static class UniqueMethodState {
        List<String> methodNames;

        @Setup(Level.Trial)
        public void setup() {
            // Generate unique method names for dynamic dispatch testing
            methodNames = new ArrayList<>();
            for (int i = 0; i < 10000; i++) {
                methodNames.add("method" + i);
            }
        }
    }

    @State(Scope.Benchmark)
    public static class MultiTypeState {
        List<List<MemoryReceiver>> receiverBatches;
        final Random random = new Random(42);

        @Setup(Level.Trial)
        public void setup() {
            // Create batches of receivers with varying type counts
            receiverBatches = new ArrayList<>();

            // Batch with 2 types
            receiverBatches.add(MemoryHelper.createReceiversWithNTypes(100, 2));
            // Batch with 4 types
            receiverBatches.add(MemoryHelper.createReceiversWithNTypes(100, 4));
            // Batch with 8 types
            receiverBatches.add(MemoryHelper.createReceiversWithNTypes(100, 8));
        }
    }

    @State(Scope.Benchmark)
    public static class CacheChurnState {
        List<MemoryReceiver> receivers;
        final Random random = new Random(42);

        @Setup(Level.Iteration)
        public void setup() {
            // Create receivers in random order to maximize cache churn
            receivers = MemoryHelper.createMixedReceivers(1000);
            Collections.shuffle(receivers, random);
        }
    }

    // ========================================================================
    // Unique method callsite benchmarks
    // ========================================================================

    /**
     * Call N unique method names dynamically.
     * Each unique method name creates a distinct callsite.
     * <p>
     * Memory cost: ~N callsites × 4 cache entries × 32 bytes per AtomicLong
     */
    @Benchmark
    public void callsite_uniqueMethods(UniqueMethodState state, Blackhole bh) {
        MemoryTestService service = new MemoryTestService();
        List<String> methodsToCall = state.methodNames.subList(0, callsiteCount);
        MemoryHelper.callUniqueMethods(service, methodsToCall, bh);
    }

    // ========================================================================
    // Unique receiver type callsite benchmarks
    // ========================================================================

    /**
     * Call same method on objects of different types.
     * Tests cache invalidation cost as types change.
     * <p>
     * With 4-entry LRU cache, types beyond 4 cause eviction and re-population.
     */
    @Benchmark
    public void callsite_uniqueReceiverTypes2(MultiTypeState state, Blackhole bh) {
        List<MemoryReceiver> receivers = state.receiverBatches.get(0);
        for (int i = 0; i < callsiteCount && i < receivers.size(); i++) {
            bh.consume(receivers.get(i % receivers.size()).receive());
        }
    }

    @Benchmark
    public void callsite_uniqueReceiverTypes4(MultiTypeState state, Blackhole bh) {
        List<MemoryReceiver> receivers = state.receiverBatches.get(1);
        for (int i = 0; i < callsiteCount && i < receivers.size(); i++) {
            bh.consume(receivers.get(i % receivers.size()).receive());
        }
    }

    @Benchmark
    public void callsite_uniqueReceiverTypes8(MultiTypeState state, Blackhole bh) {
        List<MemoryReceiver> receivers = state.receiverBatches.get(2);
        for (int i = 0; i < callsiteCount && i < receivers.size(); i++) {
            bh.consume(receivers.get(i % receivers.size()).receive());
        }
    }

    // ========================================================================
    // Cache churn benchmarks
    // ========================================================================

    /**
     * Repeatedly invalidate caches with type changes.
     * Simulates worst-case scenario: random type order maximizes cache misses.
     */
    @Benchmark
    public void callsite_cacheChurn(CacheChurnState state, Blackhole bh) {
        List<MemoryReceiver> receivers = state.receivers;
        int iterations = Math.min(callsiteCount, receivers.size());
        for (int round = 0; round < 10; round++) {
            for (int i = 0; i < iterations; i++) {
                bh.consume(receivers.get(i).receive());
                bh.consume(receivers.get(i).getValue());
            }
        }
    }

    /**
     * Alternating types to trigger continuous cache updates.
     * Pattern: A, B, C, D, E, F, G, H, A, B, ... (8 types rotating)
     */
    @Benchmark
    public void callsite_rotatingTypes(CacheChurnState state, Blackhole bh) {
        List<MemoryReceiver> receivers = state.receivers;
        for (int i = 0; i < callsiteCount; i++) {
            // Access receivers in type-rotating order
            int index = i % 8; // 8 different types
            MemoryReceiver r = receivers.get(index * (receivers.size() / 8));
            bh.consume(r.receive());
        }
    }

    // ========================================================================
    // Groovy collection operation callsite benchmarks
    // ========================================================================

    /**
     * Spread operator creates callsites per element type.
     */
    @Benchmark
    public void callsite_spreadGrowth(MultiTypeState state, Blackhole bh) {
        for (int i = 0; i < callsiteCount / 100; i++) {
            for (List<MemoryReceiver> batch : state.receiverBatches) {
                MemoryHelper.spreadOperatorOverhead(batch, bh);
            }
        }
    }

    /**
     * Closure operations create callsites within closure scope.
     */
    @Benchmark
    public void callsite_closureGrowth(MultiTypeState state, Blackhole bh) {
        for (int i = 0; i < callsiteCount / 100; i++) {
            for (List<MemoryReceiver> batch : state.receiverBatches) {
                MemoryHelper.closureOverhead(batch, bh);
            }
        }
    }

    // ========================================================================
    // Java baseline benchmarks
    // ========================================================================

    /**
     * Java baseline: interface dispatch (no callsite caching overhead).
     */
    @Benchmark
    public void java_interfaceDispatch(MultiTypeState state, Blackhole bh) {
        List<MemoryReceiver> receivers = state.receiverBatches.get(2); // 8 types
        for (int i = 0; i < callsiteCount && i < receivers.size(); i++) {
            MemoryReceiver r = receivers.get(i % receivers.size());
            bh.consume(r.receive());
            bh.consume(r.getValue());
        }
    }

    /**
     * Java baseline: rotating types through interface.
     */
    @Benchmark
    public void java_rotatingTypes(CacheChurnState state, Blackhole bh) {
        List<MemoryReceiver> receivers = state.receivers;
        for (int i = 0; i < callsiteCount; i++) {
            int index = i % 8;
            MemoryReceiver r = receivers.get(index * (receivers.size() / 8));
            bh.consume(r.receive());
        }
    }
}
