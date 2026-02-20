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
 * Benchmarks for object graph navigation patterns common in GORM/Grails.
 * <p>
 * Tests patterns like:
 * - person.address.city
 * - order.customer.address.country
 * - order.items*.product.category
 * <p>
 * Run with: ./gradlew -Pindy=true -PbenchInclude=EntityTraversal :perf:jmh
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class EntityTraversalBench {

    @State(Scope.Thread)
    public static class TraversalState {
        EntityGraph graph;

        @Setup(Level.Trial)
        public void setup() {
            graph = new EntityGraph();
            graph.buildGraph(100); // 100 customers with orders
        }
    }

    // ========================================================================
    // Simple traversal (2 levels)
    // ========================================================================

    @Benchmark
    public void traversal_simpleProperty(TraversalState state, Blackhole bh) {
        state.graph.traverseSimpleProperty(bh);
    }

    @Benchmark
    public void traversal_nestedProperty(TraversalState state, Blackhole bh) {
        state.graph.traverseNestedProperty(bh);
    }

    // ========================================================================
    // Deep traversal (3+ levels)
    // ========================================================================

    @Benchmark
    public void traversal_deepProperty(TraversalState state, Blackhole bh) {
        state.graph.traverseDeepProperty(bh);
    }

    @Benchmark
    public void traversal_veryDeepProperty(TraversalState state, Blackhole bh) {
        state.graph.traverseVeryDeepProperty(bh);
    }

    // ========================================================================
    // Collection traversal with spread
    // ========================================================================

    @Benchmark
    public void traversal_spreadOnCollection(TraversalState state, Blackhole bh) {
        state.graph.traverseWithSpread(bh);
    }

    @Benchmark
    public void traversal_nestedSpread(TraversalState state, Blackhole bh) {
        state.graph.traverseNestedSpread(bh);
    }

    // ========================================================================
    // Null-safe traversal
    // ========================================================================

    @Benchmark
    public void traversal_nullSafe(TraversalState state, Blackhole bh) {
        state.graph.traverseNullSafe(bh);
    }

    // ========================================================================
    // Mixed traversal patterns
    // ========================================================================

    @Benchmark
    public void traversal_mixedPatterns(TraversalState state, Blackhole bh) {
        state.graph.traverseMixedPatterns(bh);
    }

    // ========================================================================
    // Java baseline
    // ========================================================================

    @Benchmark
    public void java_simpleTraversal(TraversalState state, Blackhole bh) {
        state.graph.javaTraverseSimple(bh);
    }

    @Benchmark
    public void java_deepTraversal(TraversalState state, Blackhole bh) {
        state.graph.javaTraverseDeep(bh);
    }
}
