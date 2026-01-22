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
package org.apache.groovy.bench.indy;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Benchmarks for measuring warmup behavior around the indy optimization threshold.
 * <p>
 * The default threshold is controlled by groovy.indy.optimize.threshold (default: 10,000).
 * This benchmark tests performance at various call counts to understand:
 * <ul>
 *   <li>The cost of calls below the threshold</li>
 *   <li>The transition behavior around the threshold</li>
 *   <li>The performance after optimization kicks in</li>
 * </ul>
 * <p>
 * Run with different thresholds:
 * <pre>
 * ./gradlew -Pindy=true -PbenchInclude=WarmupBehavior :perf:jmh
 * ./gradlew -Pindy=true -PbenchInclude=WarmupBehavior :perf:jmh -Dgroovy.indy.optimize.threshold=100
 * </pre>
 */
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(3)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class WarmupBehaviorBench {

    // ========================================================================
    // State classes with different warmup levels
    // ========================================================================

    @State(Scope.Thread)
    public static class ColdState {
        SimpleService service;

        @Setup(Level.Invocation)
        public void setup() {
            // Fresh object each time - completely cold callsite
            service = new SimpleService();
        }
    }

    @State(Scope.Thread)
    public static class Warmed10State {
        SimpleService service;

        @Setup(Level.Iteration)
        public void setup() {
            service = new SimpleService();
            // Warm with 10 calls
            for (int i = 0; i < 10; i++) {
                service.getName();
            }
        }
    }

    @State(Scope.Thread)
    public static class Warmed100State {
        SimpleService service;

        @Setup(Level.Iteration)
        public void setup() {
            service = new SimpleService();
            // Warm with 100 calls
            for (int i = 0; i < 100; i++) {
                service.getName();
            }
        }
    }

    @State(Scope.Thread)
    public static class Warmed1000State {
        SimpleService service;

        @Setup(Level.Iteration)
        public void setup() {
            service = new SimpleService();
            // Warm with 1,000 calls
            for (int i = 0; i < 1000; i++) {
                service.getName();
            }
        }
    }

    @State(Scope.Thread)
    public static class Warmed5000State {
        SimpleService service;

        @Setup(Level.Iteration)
        public void setup() {
            service = new SimpleService();
            // Warm with 5,000 calls (below default threshold)
            for (int i = 0; i < 5000; i++) {
                service.getName();
            }
        }
    }

    @State(Scope.Thread)
    public static class Warmed10000State {
        SimpleService service;

        @Setup(Level.Iteration)
        public void setup() {
            service = new SimpleService();
            // Warm with 10,000 calls (at default threshold)
            for (int i = 0; i < 10000; i++) {
                service.getName();
            }
        }
    }

    @State(Scope.Thread)
    public static class Warmed15000State {
        SimpleService service;

        @Setup(Level.Iteration)
        public void setup() {
            service = new SimpleService();
            // Warm with 15,000 calls (above default threshold)
            for (int i = 0; i < 15000; i++) {
                service.getName();
            }
        }
    }

    @State(Scope.Thread)
    public static class Warmed50000State {
        SimpleService service;

        @Setup(Level.Iteration)
        public void setup() {
            service = new SimpleService();
            // Warm with 50,000 calls (well above threshold)
            for (int i = 0; i < 50000; i++) {
                service.getName();
            }
        }
    }

    @State(Scope.Thread)
    public static class FullyWarmedState {
        SimpleService service;

        @Setup(Level.Trial)
        public void setup() {
            service = new SimpleService();
            // Warm with 200,000 calls (fully optimized)
            for (int i = 0; i < 200000; i++) {
                service.getName();
            }
        }
    }

    // ========================================================================
    // Benchmarks at different warmup levels
    // ========================================================================

    @Benchmark
    public Object warmup_00_cold(ColdState state, Blackhole bh) {
        Object result = state.service.getName();
        bh.consume(result);
        return result;
    }

    @Benchmark
    public Object warmup_01_after10(Warmed10State state, Blackhole bh) {
        Object result = state.service.getName();
        bh.consume(result);
        return result;
    }

    @Benchmark
    public Object warmup_02_after100(Warmed100State state, Blackhole bh) {
        Object result = state.service.getName();
        bh.consume(result);
        return result;
    }

    @Benchmark
    public Object warmup_03_after1000(Warmed1000State state, Blackhole bh) {
        Object result = state.service.getName();
        bh.consume(result);
        return result;
    }

    @Benchmark
    public Object warmup_04_after5000(Warmed5000State state, Blackhole bh) {
        Object result = state.service.getName();
        bh.consume(result);
        return result;
    }

    @Benchmark
    public Object warmup_05_after10000(Warmed10000State state, Blackhole bh) {
        Object result = state.service.getName();
        bh.consume(result);
        return result;
    }

    @Benchmark
    public Object warmup_06_after15000(Warmed15000State state, Blackhole bh) {
        Object result = state.service.getName();
        bh.consume(result);
        return result;
    }

    @Benchmark
    public Object warmup_07_after50000(Warmed50000State state, Blackhole bh) {
        Object result = state.service.getName();
        bh.consume(result);
        return result;
    }

    @Benchmark
    public Object warmup_08_fullyWarmed(FullyWarmedState state, Blackhole bh) {
        Object result = state.service.getName();
        bh.consume(result);
        return result;
    }

    // ========================================================================
    // Java baseline for comparison
    // ========================================================================

    @State(Scope.Thread)
    public static class JavaState {
        JavaService service;

        @Setup(Level.Trial)
        public void setup() {
            service = new JavaService();
        }
    }

    public static class JavaService {
        public String getName() { return "JavaService"; }
    }

    @Benchmark
    public Object java_baseline(JavaState state, Blackhole bh) {
        Object result = state.service.getName();
        bh.consume(result);
        return result;
    }
}
