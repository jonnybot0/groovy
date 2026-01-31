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

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Benchmarks for collections containing mixed types.
 * <p>
 * This is common in Groovy/Grails where lists contain objects of
 * different domain classes (e.g., a list of search results with
 * Person, Order, Product objects).
 * <p>
 * Tests the callsite cache behavior when receiver types vary.
 * <p>
 * Run with: ./gradlew -Pindy=true -PbenchInclude=MixedTypeCollection :perf:jmh
 */
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(2)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class MixedTypeCollectionBench {

    private static final int COLLECTION_SIZE = 100;

    // ========================================================================
    // State classes with varying type diversity
    // ========================================================================

    @State(Scope.Thread)
    public static class Types2State {
        List<Object> objects;

        @Setup(Level.Trial)
        public void setup() {
            objects = MixedTypeHelper.createMixedCollection(COLLECTION_SIZE, 2);
        }
    }

    @State(Scope.Thread)
    public static class Types5State {
        List<Object> objects;

        @Setup(Level.Trial)
        public void setup() {
            objects = MixedTypeHelper.createMixedCollection(COLLECTION_SIZE, 5);
        }
    }

    @State(Scope.Thread)
    public static class Types10State {
        List<Object> objects;

        @Setup(Level.Trial)
        public void setup() {
            objects = MixedTypeHelper.createMixedCollection(COLLECTION_SIZE, 10);
        }
    }

    @State(Scope.Thread)
    public static class Types20State {
        List<Object> objects;

        @Setup(Level.Trial)
        public void setup() {
            objects = MixedTypeHelper.createMixedCollection(COLLECTION_SIZE, 20);
        }
    }

    // ========================================================================
    // Benchmarks with 2 types
    // ========================================================================

    @Benchmark
    @OperationsPerInvocation(COLLECTION_SIZE)
    public void mixed_2types_getName(Types2State state, Blackhole bh) {
        MixedTypeHelper.callGetName(state.objects, bh);
    }

    @Benchmark
    @OperationsPerInvocation(COLLECTION_SIZE)
    public void mixed_2types_getValue(Types2State state, Blackhole bh) {
        MixedTypeHelper.callGetValue(state.objects, bh);
    }

    @Benchmark
    @OperationsPerInvocation(COLLECTION_SIZE)
    public void mixed_2types_process(Types2State state, Blackhole bh) {
        MixedTypeHelper.callProcess(state.objects, bh);
    }

    // ========================================================================
    // Benchmarks with 5 types
    // ========================================================================

    @Benchmark
    @OperationsPerInvocation(COLLECTION_SIZE)
    public void mixed_5types_getName(Types5State state, Blackhole bh) {
        MixedTypeHelper.callGetName(state.objects, bh);
    }

    @Benchmark
    @OperationsPerInvocation(COLLECTION_SIZE)
    public void mixed_5types_getValue(Types5State state, Blackhole bh) {
        MixedTypeHelper.callGetValue(state.objects, bh);
    }

    @Benchmark
    @OperationsPerInvocation(COLLECTION_SIZE)
    public void mixed_5types_process(Types5State state, Blackhole bh) {
        MixedTypeHelper.callProcess(state.objects, bh);
    }

    // ========================================================================
    // Benchmarks with 10 types
    // ========================================================================

    @Benchmark
    @OperationsPerInvocation(COLLECTION_SIZE)
    public void mixed_10types_getName(Types10State state, Blackhole bh) {
        MixedTypeHelper.callGetName(state.objects, bh);
    }

    @Benchmark
    @OperationsPerInvocation(COLLECTION_SIZE)
    public void mixed_10types_getValue(Types10State state, Blackhole bh) {
        MixedTypeHelper.callGetValue(state.objects, bh);
    }

    @Benchmark
    @OperationsPerInvocation(COLLECTION_SIZE)
    public void mixed_10types_process(Types10State state, Blackhole bh) {
        MixedTypeHelper.callProcess(state.objects, bh);
    }

    // ========================================================================
    // Benchmarks with 20 types (beyond typical cache size)
    // ========================================================================

    @Benchmark
    @OperationsPerInvocation(COLLECTION_SIZE)
    public void mixed_20types_getName(Types20State state, Blackhole bh) {
        MixedTypeHelper.callGetName(state.objects, bh);
    }

    @Benchmark
    @OperationsPerInvocation(COLLECTION_SIZE)
    public void mixed_20types_getValue(Types20State state, Blackhole bh) {
        MixedTypeHelper.callGetValue(state.objects, bh);
    }

    @Benchmark
    @OperationsPerInvocation(COLLECTION_SIZE)
    public void mixed_20types_process(Types20State state, Blackhole bh) {
        MixedTypeHelper.callProcess(state.objects, bh);
    }

    // ========================================================================
    // Collection operations on mixed types
    // ========================================================================

    @Benchmark
    public Object mixed_5types_collect(Types5State state, Blackhole bh) {
        Object result = MixedTypeHelper.collectNames(state.objects);
        bh.consume(result);
        return result;
    }

    @Benchmark
    public Object mixed_10types_collect(Types10State state, Blackhole bh) {
        Object result = MixedTypeHelper.collectNames(state.objects);
        bh.consume(result);
        return result;
    }

    @Benchmark
    public Object mixed_20types_collect(Types20State state, Blackhole bh) {
        Object result = MixedTypeHelper.collectNames(state.objects);
        bh.consume(result);
        return result;
    }

    // ========================================================================
    // Java interface baseline
    // ========================================================================

    @Benchmark
    @OperationsPerInvocation(COLLECTION_SIZE)
    public void java_interface_getName(Types10State state, Blackhole bh) {
        MixedTypeHelper.javaInterfaceCall(state.objects, bh);
    }
}
