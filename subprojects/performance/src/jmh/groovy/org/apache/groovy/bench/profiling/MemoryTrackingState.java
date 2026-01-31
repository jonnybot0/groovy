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

import org.openjdk.jmh.annotations.*;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * JMH State class for tracking memory allocation and GC activity.
 * <p>
 * Use this state in benchmarks to capture memory metrics before and after
 * benchmark execution. This is particularly useful for identifying memory
 * leaks and excessive allocation in Groovy's invokedynamic implementation.
 * <p>
 * Usage:
 * <pre>
 * &#64;Benchmark
 * public void myBenchmark(MemoryTrackingState memState, Blackhole bh) {
 *     // Your benchmark code
 * }
 * </pre>
 * <p>
 * Memory metrics are printed at the end of each trial.
 */
@State(Scope.Thread)
public class MemoryTrackingState {

    // Memory tracking
    private long heapUsedBefore;
    private long heapUsedAfter;
    private long heapCommittedBefore;
    private long heapCommittedAfter;

    // GC tracking
    private long gcCountBefore;
    private long gcTimeBefore;
    private long gcCountAfter;
    private long gcTimeAfter;

    // Allocation tracking (approximate)
    private long allocationEstimateBefore;
    private long allocationEstimateAfter;

    // Trial counters
    private static final AtomicLong trialCounter = new AtomicLong(0);
    private long trialId;

    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private final List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

    /**
     * Capture memory state before each trial.
     */
    @Setup(Level.Trial)
    public void captureBeforeMemory() {
        trialId = trialCounter.incrementAndGet();

        // Force GC for consistent baseline
        System.gc();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.gc();

        // Capture heap metrics
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        heapUsedBefore = heapUsage.getUsed();
        heapCommittedBefore = heapUsage.getCommitted();

        // Capture GC metrics
        gcCountBefore = getTotalGcCount();
        gcTimeBefore = getTotalGcTime();

        // Estimate allocations
        allocationEstimateBefore = getAllocationEstimate();

        System.out.printf("[Trial %d] BEFORE - Heap: %.2f MB used, %.2f MB committed%n",
                trialId,
                heapUsedBefore / (1024.0 * 1024.0),
                heapCommittedBefore / (1024.0 * 1024.0));
    }

    /**
     * Capture memory state after each trial and report delta.
     */
    @TearDown(Level.Trial)
    public void captureAfterMemory() {
        // Capture current state (before forced GC)
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        heapUsedAfter = heapUsage.getUsed();
        heapCommittedAfter = heapUsage.getCommitted();

        gcCountAfter = getTotalGcCount();
        gcTimeAfter = getTotalGcTime();

        allocationEstimateAfter = getAllocationEstimate();

        // Calculate deltas
        long heapDelta = heapUsedAfter - heapUsedBefore;
        long gcCountDelta = gcCountAfter - gcCountBefore;
        long gcTimeDelta = gcTimeAfter - gcTimeBefore;
        long allocationDelta = allocationEstimateAfter - allocationEstimateBefore;

        System.out.printf("[Trial %d] AFTER - Heap: %.2f MB used, %.2f MB committed%n",
                trialId,
                heapUsedAfter / (1024.0 * 1024.0),
                heapCommittedAfter / (1024.0 * 1024.0));

        System.out.printf("[Trial %d] DELTA - Heap: %+.2f MB, GC: %d collections (%d ms), Est. Alloc: %.2f MB%n",
                trialId,
                heapDelta / (1024.0 * 1024.0),
                gcCountDelta,
                gcTimeDelta,
                allocationDelta / (1024.0 * 1024.0));

        // Check for potential memory leak
        if (heapDelta > 50 * 1024 * 1024) { // > 50 MB growth
            System.out.printf("[Trial %d] WARNING: Significant heap growth detected (%.2f MB)%n",
                    trialId, heapDelta / (1024.0 * 1024.0));
        }

        System.out.println();
    }

    /**
     * Capture memory at iteration boundaries for finer-grained tracking.
     */
    @Setup(Level.Iteration)
    public void beforeIteration() {
        // Optional: could add iteration-level tracking here
    }

    @TearDown(Level.Iteration)
    public void afterIteration() {
        // Optional: could add iteration-level tracking here
    }

    /**
     * Get total GC collection count across all collectors.
     */
    private long getTotalGcCount() {
        long total = 0;
        for (GarbageCollectorMXBean gc : gcBeans) {
            long count = gc.getCollectionCount();
            if (count >= 0) {
                total += count;
            }
        }
        return total;
    }

    /**
     * Get total GC time across all collectors.
     */
    private long getTotalGcTime() {
        long total = 0;
        for (GarbageCollectorMXBean gc : gcBeans) {
            long time = gc.getCollectionTime();
            if (time >= 0) {
                total += time;
            }
        }
        return total;
    }

    /**
     * Estimate total allocation since JVM start.
     * This is approximate and depends on available MXBeans.
     */
    private long getAllocationEstimate() {
        // Use committed memory as a rough estimate
        // More accurate tracking would require additional tooling
        return memoryBean.getHeapMemoryUsage().getCommitted();
    }

    // Getters for use in benchmarks
    public long getHeapUsedBefore() { return heapUsedBefore; }
    public long getHeapUsedAfter() { return heapUsedAfter; }
    public long getHeapDelta() { return heapUsedAfter - heapUsedBefore; }
    public long getGcCount() { return gcCountAfter - gcCountBefore; }
    public long getGcTime() { return gcTimeAfter - gcTimeBefore; }

    /**
     * Force a GC and return the heap used after.
     * Useful for measuring retained memory.
     */
    public long getRetainedMemory() {
        System.gc();
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.gc();
        return memoryBean.getHeapMemoryUsage().getUsed();
    }
}
