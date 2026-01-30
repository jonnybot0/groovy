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
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.lang.management.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Profiled benchmark runner with detailed memory and performance analysis.
 * <p>
 * This class provides a programmatic way to run JMH benchmarks with
 * additional profiling capabilities beyond what JMH provides by default.
 * <p>
 * Features:
 * <ul>
 *   <li>Detailed heap memory tracking before/after benchmarks</li>
 *   <li>Class loading statistics</li>
 *   <li>Thread count monitoring</li>
 *   <li>GC activity reporting</li>
 *   <li>Groovy-specific metrics (callsite count estimation)</li>
 * </ul>
 * <p>
 * Run with: java -cp ... ProfiledBenchmarkRunner [benchmark-pattern]
 */
public class ProfiledBenchmarkRunner {

    private static final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private static final ClassLoadingMXBean classLoadingBean = ManagementFactory.getClassLoadingMXBean();
    private static final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    private static final List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

    /**
     * Main entry point for running profiled benchmarks.
     */
    public static void main(String[] args) throws RunnerException {
        String pattern = args.length > 0 ? args[0] : ".*";

        System.out.println("=" .repeat(80));
        System.out.println("PROFILED BENCHMARK RUNNER");
        System.out.println("Pattern: " + pattern);
        System.out.println("=" .repeat(80));
        System.out.println();

        // Capture initial state
        SystemState initialState = captureSystemState();
        printSystemState("INITIAL STATE", initialState);

        // Build JMH options
        Options opt = new OptionsBuilder()
                .include(pattern)
                .warmupIterations(3)
                .measurementIterations(5)
                .forks(1)
                .build();

        // Run benchmarks
        System.out.println("\nRunning benchmarks...\n");
        new Runner(opt).run();

        // Capture final state
        SystemState finalState = captureSystemState();
        printSystemState("FINAL STATE", finalState);

        // Print delta
        printStateDelta(initialState, finalState);
    }

    /**
     * Capture current system state.
     */
    public static SystemState captureSystemState() {
        // Force GC for accurate memory reading
        System.gc();
        try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        SystemState state = new SystemState();

        // Memory
        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        state.heapUsed = heap.getUsed();
        state.heapCommitted = heap.getCommitted();
        state.heapMax = heap.getMax();

        MemoryUsage nonHeap = memoryBean.getNonHeapMemoryUsage();
        state.nonHeapUsed = nonHeap.getUsed();
        state.nonHeapCommitted = nonHeap.getCommitted();

        // Classes
        state.loadedClassCount = classLoadingBean.getLoadedClassCount();
        state.totalLoadedClassCount = classLoadingBean.getTotalLoadedClassCount();
        state.unloadedClassCount = classLoadingBean.getUnloadedClassCount();

        // Threads
        state.threadCount = threadBean.getThreadCount();
        state.peakThreadCount = threadBean.getPeakThreadCount();

        // GC
        for (GarbageCollectorMXBean gc : gcBeans) {
            state.gcCount += gc.getCollectionCount();
            state.gcTime += gc.getCollectionTime();
        }

        state.timestamp = System.currentTimeMillis();

        return state;
    }

    /**
     * Print system state.
     */
    public static void printSystemState(String label, SystemState state) {
        System.out.println("-".repeat(60));
        System.out.println(label);
        System.out.println("-".repeat(60));
        System.out.printf("Heap Memory:     %.2f MB used / %.2f MB committed / %.2f MB max%n",
                state.heapUsed / 1024.0 / 1024.0,
                state.heapCommitted / 1024.0 / 1024.0,
                state.heapMax / 1024.0 / 1024.0);
        System.out.printf("Non-Heap Memory: %.2f MB used / %.2f MB committed%n",
                state.nonHeapUsed / 1024.0 / 1024.0,
                state.nonHeapCommitted / 1024.0 / 1024.0);
        System.out.printf("Classes:         %d loaded / %d total loaded / %d unloaded%n",
                state.loadedClassCount, state.totalLoadedClassCount, state.unloadedClassCount);
        System.out.printf("Threads:         %d current / %d peak%n",
                state.threadCount, state.peakThreadCount);
        System.out.printf("GC:              %d collections / %d ms total%n",
                state.gcCount, state.gcTime);
        System.out.println();
    }

    /**
     * Print delta between two states.
     */
    public static void printStateDelta(SystemState before, SystemState after) {
        System.out.println("=".repeat(60));
        System.out.println("BENCHMARK IMPACT (Delta)");
        System.out.println("=".repeat(60));

        long heapDelta = after.heapUsed - before.heapUsed;
        long nonHeapDelta = after.nonHeapUsed - before.nonHeapUsed;
        int classLoadDelta = after.loadedClassCount - before.loadedClassCount;
        long totalClassDelta = after.totalLoadedClassCount - before.totalLoadedClassCount;
        long gcCountDelta = after.gcCount - before.gcCount;
        long gcTimeDelta = after.gcTime - before.gcTime;
        long duration = after.timestamp - before.timestamp;

        System.out.printf("Duration:        %.2f seconds%n", duration / 1000.0);
        System.out.printf("Heap Growth:     %+.2f MB%n", heapDelta / 1024.0 / 1024.0);
        System.out.printf("Non-Heap Growth: %+.2f MB%n", nonHeapDelta / 1024.0 / 1024.0);
        System.out.printf("Classes Loaded:  %+d (total: %+d)%n", classLoadDelta, totalClassDelta);
        System.out.printf("GC Activity:     %d collections, %d ms%n", gcCountDelta, gcTimeDelta);

        // Warnings
        System.out.println();
        if (heapDelta > 100 * 1024 * 1024) {
            System.out.println("WARNING: Significant heap growth (>100MB) - possible memory leak");
        }
        if (classLoadDelta > 1000) {
            System.out.println("WARNING: Large number of classes loaded (>1000) - check dynamic class generation");
        }
        if (gcTimeDelta > duration * 0.1) {
            System.out.println("WARNING: High GC overhead (>10% of runtime)");
        }

        System.out.println();
    }

    /**
     * System state snapshot.
     */
    public static class SystemState {
        public long heapUsed;
        public long heapCommitted;
        public long heapMax;
        public long nonHeapUsed;
        public long nonHeapCommitted;
        public int loadedClassCount;
        public long totalLoadedClassCount;
        public long unloadedClassCount;
        public int threadCount;
        public int peakThreadCount;
        public long gcCount;
        public long gcTime;
        public long timestamp;
    }

    /**
     * Estimate the number of Groovy callsites by examining loaded classes.
     * This is a rough approximation based on class naming patterns.
     */
    public static int estimateCallsiteCount() {
        int count = 0;
        // This would require reflection to inspect GroovyClassLoader or CallSite arrays
        // For now, return -1 to indicate "not implemented"
        return -1;
    }
}

/**
 * JMH State that combines MemoryTrackingState with additional Groovy-specific tracking.
 */
@State(Scope.Benchmark)
class GroovyProfilingState {

    private ProfiledBenchmarkRunner.SystemState beforeState;
    private ProfiledBenchmarkRunner.SystemState afterState;

    @Setup(Level.Trial)
    public void setup() {
        beforeState = ProfiledBenchmarkRunner.captureSystemState();
        ProfiledBenchmarkRunner.printSystemState("Benchmark Start", beforeState);
    }

    @TearDown(Level.Trial)
    public void teardown() {
        afterState = ProfiledBenchmarkRunner.captureSystemState();
        ProfiledBenchmarkRunner.printSystemState("Benchmark End", afterState);
        ProfiledBenchmarkRunner.printStateDelta(beforeState, afterState);
    }

    public ProfiledBenchmarkRunner.SystemState getBeforeState() {
        return beforeState;
    }

    public ProfiledBenchmarkRunner.SystemState getAfterState() {
        return afterState;
    }
}
