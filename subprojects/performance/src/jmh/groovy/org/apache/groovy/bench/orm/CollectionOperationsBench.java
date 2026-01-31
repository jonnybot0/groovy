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
package org.apache.groovy.bench.orm;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Benchmarks for Groovy collection operations on domain objects.
 * <p>
 * Tests common GORM/Grails patterns:
 * - .each { } iteration
 * - .collect { } transformation
 * - .findAll { } filtering
 * - .groupBy { } aggregation
 * - .sum { }, .max { }, .min { }
 * <p>
 * Run with: ./gradlew -Pindy=true -PbenchInclude=CollectionOperations :perf:jmh
 */
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(2)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class CollectionOperationsBench {

    private static final int COLLECTION_SIZE = 1000;

    @State(Scope.Thread)
    public static class CollectionState {
        CollectionOps ops;
        List<CollectionDomain> domains;

        @Setup(Level.Trial)
        public void setup() {
            ops = new CollectionOps();
            domains = ops.createDomains(COLLECTION_SIZE);
        }
    }

    // ========================================================================
    // Basic iteration
    // ========================================================================

    @Benchmark
    public void collection_each(CollectionState state, Blackhole bh) {
        state.ops.doEach(state.domains, bh);
    }

    @Benchmark
    public void collection_eachWithIndex(CollectionState state, Blackhole bh) {
        state.ops.doEachWithIndex(state.domains, bh);
    }

    // ========================================================================
    // Transformation
    // ========================================================================

    @Benchmark
    public Object collection_collect(CollectionState state, Blackhole bh) {
        Object result = state.ops.doCollect(state.domains);
        bh.consume(result);
        return result;
    }

    @Benchmark
    public Object collection_collectNested(CollectionState state, Blackhole bh) {
        Object result = state.ops.doCollectNested(state.domains);
        bh.consume(result);
        return result;
    }

    @Benchmark
    public Object collection_collectMany(CollectionState state, Blackhole bh) {
        Object result = state.ops.doCollectMany(state.domains);
        bh.consume(result);
        return result;
    }

    // ========================================================================
    // Filtering
    // ========================================================================

    @Benchmark
    public Object collection_findAll(CollectionState state, Blackhole bh) {
        Object result = state.ops.doFindAll(state.domains);
        bh.consume(result);
        return result;
    }

    @Benchmark
    public Object collection_find(CollectionState state, Blackhole bh) {
        Object result = state.ops.doFind(state.domains);
        bh.consume(result);
        return result;
    }

    @Benchmark
    public Object collection_grep(CollectionState state, Blackhole bh) {
        Object result = state.ops.doGrep(state.domains);
        bh.consume(result);
        return result;
    }

    // ========================================================================
    // Aggregation
    // ========================================================================

    @Benchmark
    public Object collection_groupBy(CollectionState state, Blackhole bh) {
        Object result = state.ops.doGroupBy(state.domains);
        bh.consume(result);
        return result;
    }

    @Benchmark
    public Object collection_countBy(CollectionState state, Blackhole bh) {
        Object result = state.ops.doCountBy(state.domains);
        bh.consume(result);
        return result;
    }

    @Benchmark
    public Object collection_sum(CollectionState state, Blackhole bh) {
        Object result = state.ops.doSum(state.domains);
        bh.consume(result);
        return result;
    }

    @Benchmark
    public Object collection_max(CollectionState state, Blackhole bh) {
        Object result = state.ops.doMax(state.domains);
        bh.consume(result);
        return result;
    }

    // ========================================================================
    // Chained operations (common in Grails)
    // ========================================================================

    @Benchmark
    public Object collection_chainedOps(CollectionState state, Blackhole bh) {
        Object result = state.ops.doChainedOperations(state.domains);
        bh.consume(result);
        return result;
    }

    @Benchmark
    public Object collection_complexChain(CollectionState state, Blackhole bh) {
        Object result = state.ops.doComplexChain(state.domains);
        bh.consume(result);
        return result;
    }

    // ========================================================================
    // Sorting
    // ========================================================================

    @Benchmark
    public Object collection_sort(CollectionState state, Blackhole bh) {
        Object result = state.ops.doSort(state.domains);
        bh.consume(result);
        return result;
    }

    @Benchmark
    public Object collection_sortBy(CollectionState state, Blackhole bh) {
        Object result = state.ops.doSortBy(state.domains);
        bh.consume(result);
        return result;
    }

    // ========================================================================
    // Java Stream baseline
    // ========================================================================

    @Benchmark
    public Object java_streamCollect(CollectionState state, Blackhole bh) {
        Object result = state.ops.javaStreamCollect(state.domains);
        bh.consume(result);
        return result;
    }

    @Benchmark
    public Object java_streamFilter(CollectionState state, Blackhole bh) {
        Object result = state.ops.javaStreamFilter(state.domains);
        bh.consume(result);
        return result;
    }

    @Benchmark
    public Object java_streamChained(CollectionState state, Blackhole bh) {
        Object result = state.ops.javaStreamChained(state.domains);
        bh.consume(result);
        return result;
    }
}
