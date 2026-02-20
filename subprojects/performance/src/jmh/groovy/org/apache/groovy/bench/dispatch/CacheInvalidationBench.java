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
package org.apache.groovy.bench.dispatch;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Benchmarks for measuring inline cache invalidation overhead.
 * <p>
 * This addresses the issues described in GROOVY-8298 where polymorphic
 * dispatch on collections with mixed types causes continuous cache
 * invalidation and prevents JIT optimization.
 * <p>
 * Key scenarios tested:
 * <ul>
 *   <li>Stable type pattern: same types in consistent order</li>
 *   <li>Rotating types: types change but predictably</li>
 *   <li>Random types: unpredictable type changes (cache thrashing)</li>
 *   <li>Growing polymorphism: gradually increasing type diversity</li>
 * </ul>
 * <p>
 * Run with: ./gradlew -Pindy=true -PbenchInclude=CacheInvalidation :perf:jmh
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class CacheInvalidationBench {

    // ========================================================================
    // Receiver classes (same interface, different implementations)
    // ========================================================================

    interface Receiver {
        String receive();
    }

    static class ReceiverA implements Receiver {
        @Override public String receive() { return "A"; }
    }

    static class ReceiverB implements Receiver {
        @Override public String receive() { return "B"; }
    }

    static class ReceiverC implements Receiver {
        @Override public String receive() { return "C"; }
    }

    static class ReceiverD implements Receiver {
        @Override public String receive() { return "D"; }
    }

    static class ReceiverE implements Receiver {
        @Override public String receive() { return "E"; }
    }

    static class ReceiverF implements Receiver {
        @Override public String receive() { return "F"; }
    }

    static class ReceiverG implements Receiver {
        @Override public String receive() { return "G"; }
    }

    static class ReceiverH implements Receiver {
        @Override public String receive() { return "H"; }
    }

    static class ReceiverI implements Receiver {
        @Override public String receive() { return "I"; }
    }

    static class ReceiverJ implements Receiver {
        @Override public String receive() { return "J"; }
    }

    private static final Receiver[] ALL_RECEIVERS = {
            new ReceiverA(), new ReceiverB(), new ReceiverC(), new ReceiverD(),
            new ReceiverE(), new ReceiverF(), new ReceiverG(), new ReceiverH(),
            new ReceiverI(), new ReceiverJ()
    };

    // ========================================================================
    // State classes
    // ========================================================================

    private static final int COLLECTION_SIZE = 100;

    /**
     * Monomorphic: all same type - best case for inline cache.
     */
    @State(Scope.Thread)
    public static class MonomorphicState {
        List<Receiver> receivers;

        @Setup(Level.Trial)
        public void setup() {
            receivers = new ArrayList<>(COLLECTION_SIZE);
            for (int i = 0; i < COLLECTION_SIZE; i++) {
                receivers.add(new ReceiverA());
            }
        }
    }

    /**
     * Bimorphic: two types - still optimizable by JIT.
     */
    @State(Scope.Thread)
    public static class BimorphicState {
        List<Receiver> receivers;

        @Setup(Level.Trial)
        public void setup() {
            receivers = new ArrayList<>(COLLECTION_SIZE);
            for (int i = 0; i < COLLECTION_SIZE; i++) {
                receivers.add(i % 2 == 0 ? new ReceiverA() : new ReceiverB());
            }
        }
    }

    /**
     * Polymorphic (3 types): getting harder for JIT.
     */
    @State(Scope.Thread)
    public static class Polymorphic3State {
        List<Receiver> receivers;

        @Setup(Level.Trial)
        public void setup() {
            receivers = new ArrayList<>(COLLECTION_SIZE);
            Receiver[] types = {new ReceiverA(), new ReceiverB(), new ReceiverC()};
            for (int i = 0; i < COLLECTION_SIZE; i++) {
                receivers.add(types[i % 3]);
            }
        }
    }

    /**
     * Megamorphic (8 types): beyond inline cache capacity.
     */
    @State(Scope.Thread)
    public static class MegamorphicState {
        List<Receiver> receivers;

        @Setup(Level.Trial)
        public void setup() {
            receivers = new ArrayList<>(COLLECTION_SIZE);
            for (int i = 0; i < COLLECTION_SIZE; i++) {
                receivers.add(ALL_RECEIVERS[i % 8]);
            }
        }
    }

    /**
     * Highly megamorphic (10 types).
     */
    @State(Scope.Thread)
    public static class HighlyMegamorphicState {
        List<Receiver> receivers;

        @Setup(Level.Trial)
        public void setup() {
            receivers = new ArrayList<>(COLLECTION_SIZE);
            for (int i = 0; i < COLLECTION_SIZE; i++) {
                receivers.add(ALL_RECEIVERS[i % 10]);
            }
        }
    }

    /**
     * Random order: unpredictable types cause cache misses.
     */
    @State(Scope.Thread)
    public static class RandomOrderState {
        List<Receiver> receivers;
        final Random random = new Random(42); // Fixed seed for reproducibility

        @Setup(Level.Iteration)
        public void setup() {
            receivers = new ArrayList<>(COLLECTION_SIZE);
            for (int i = 0; i < COLLECTION_SIZE; i++) {
                receivers.add(ALL_RECEIVERS[random.nextInt(8)]);
            }
            // Shuffle to ensure random access pattern
            Collections.shuffle(receivers, random);
        }
    }

    /**
     * Changing types each iteration: simulates cache invalidation.
     */
    @State(Scope.Thread)
    public static class ChangingTypesState {
        List<Receiver> receivers;
        int iteration = 0;

        @Setup(Level.Iteration)
        public void setup() {
            receivers = new ArrayList<>(COLLECTION_SIZE);
            // Each iteration uses different types
            int offset = iteration % 5;
            for (int i = 0; i < COLLECTION_SIZE; i++) {
                receivers.add(ALL_RECEIVERS[(i + offset) % 8]);
            }
            iteration++;
        }
    }

    // ========================================================================
    // Groovy dispatch benchmarks
    // ========================================================================

    @Benchmark
    @OperationsPerInvocation(COLLECTION_SIZE)
    public void groovy_monomorphic(MonomorphicState state, Blackhole bh) {
        DispatchHelper.dispatchAll(state.receivers, bh);
    }

    @Benchmark
    @OperationsPerInvocation(COLLECTION_SIZE)
    public void groovy_bimorphic(BimorphicState state, Blackhole bh) {
        DispatchHelper.dispatchAll(state.receivers, bh);
    }

    @Benchmark
    @OperationsPerInvocation(COLLECTION_SIZE)
    public void groovy_polymorphic3(Polymorphic3State state, Blackhole bh) {
        DispatchHelper.dispatchAll(state.receivers, bh);
    }

    @Benchmark
    @OperationsPerInvocation(COLLECTION_SIZE)
    public void groovy_megamorphic8(MegamorphicState state, Blackhole bh) {
        DispatchHelper.dispatchAll(state.receivers, bh);
    }

    @Benchmark
    @OperationsPerInvocation(COLLECTION_SIZE)
    public void groovy_megamorphic10(HighlyMegamorphicState state, Blackhole bh) {
        DispatchHelper.dispatchAll(state.receivers, bh);
    }

    @Benchmark
    @OperationsPerInvocation(COLLECTION_SIZE)
    public void groovy_randomOrder(RandomOrderState state, Blackhole bh) {
        DispatchHelper.dispatchAll(state.receivers, bh);
    }

    @Benchmark
    @OperationsPerInvocation(COLLECTION_SIZE)
    public void groovy_changingTypes(ChangingTypesState state, Blackhole bh) {
        DispatchHelper.dispatchAll(state.receivers, bh);
    }

    // ========================================================================
    // Java baseline benchmarks
    // ========================================================================

    @Benchmark
    @OperationsPerInvocation(COLLECTION_SIZE)
    public void java_monomorphic(MonomorphicState state, Blackhole bh) {
        for (Receiver r : state.receivers) {
            bh.consume(r.receive());
        }
    }

    @Benchmark
    @OperationsPerInvocation(COLLECTION_SIZE)
    public void java_bimorphic(BimorphicState state, Blackhole bh) {
        for (Receiver r : state.receivers) {
            bh.consume(r.receive());
        }
    }

    @Benchmark
    @OperationsPerInvocation(COLLECTION_SIZE)
    public void java_polymorphic3(Polymorphic3State state, Blackhole bh) {
        for (Receiver r : state.receivers) {
            bh.consume(r.receive());
        }
    }

    @Benchmark
    @OperationsPerInvocation(COLLECTION_SIZE)
    public void java_megamorphic8(MegamorphicState state, Blackhole bh) {
        for (Receiver r : state.receivers) {
            bh.consume(r.receive());
        }
    }

    @Benchmark
    @OperationsPerInvocation(COLLECTION_SIZE)
    public void java_megamorphic10(HighlyMegamorphicState state, Blackhole bh) {
        for (Receiver r : state.receivers) {
            bh.consume(r.receive());
        }
    }

    @Benchmark
    @OperationsPerInvocation(COLLECTION_SIZE)
    public void java_randomOrder(RandomOrderState state, Blackhole bh) {
        for (Receiver r : state.receivers) {
            bh.consume(r.receive());
        }
    }

    @Benchmark
    @OperationsPerInvocation(COLLECTION_SIZE)
    public void java_changingTypes(ChangingTypesState state, Blackhole bh) {
        for (Receiver r : state.receivers) {
            bh.consume(r.receive());
        }
    }
}
