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
 * Benchmarks simulating GORM dynamic finder patterns.
 * <p>
 * Dynamic finders like findByName(), findAllByStatus() use methodMissing
 * which exercises Groovy's dynamic dispatch heavily.
 * <p>
 * Run with: ./gradlew -Pindy=true -PbenchInclude=DynamicFinder :perf:jmh
 */
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(2)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class DynamicFinderBench {

    @State(Scope.Thread)
    public static class FinderState {
        DynamicFinderService service;
        Random random;

        @Setup(Level.Trial)
        public void setup() {
            random = new Random(42);
            service = new DynamicFinderService();

            // Seed with test data
            for (int i = 0; i < 1000; i++) {
                service.addPerson("First" + i, "Last" + (i % 100), "person" + i + "@example.com", i % 50);
            }
        }
    }

    // ========================================================================
    // Single property finders
    // ========================================================================

    @Benchmark
    public Object finder_findByEmail(FinderState state, Blackhole bh) {
        Object result = DynamicFinderHelper.findByEmail(state.service, "person42@example.com");
        bh.consume(result);
        return result;
    }

    @Benchmark
    public Object finder_findByLastName(FinderState state, Blackhole bh) {
        Object result = DynamicFinderHelper.findByLastName(state.service, "Last25");
        bh.consume(result);
        return result;
    }

    @Benchmark
    public Object finder_findByAge(FinderState state, Blackhole bh) {
        Object result = DynamicFinderHelper.findByAge(state.service, 30);
        bh.consume(result);
        return result;
    }

    // ========================================================================
    // FindAll finders (return lists)
    // ========================================================================

    @Benchmark
    public Object finder_findAllByLastName(FinderState state, Blackhole bh) {
        Object result = DynamicFinderHelper.findAllByLastName(state.service, "Last25");
        bh.consume(result);
        return result;
    }

    @Benchmark
    public Object finder_findAllByAge(FinderState state, Blackhole bh) {
        Object result = DynamicFinderHelper.findAllByAge(state.service, 30);
        bh.consume(result);
        return result;
    }

    // ========================================================================
    // Compound finders
    // ========================================================================

    @Benchmark
    public Object finder_findByFirstNameAndLastName(FinderState state, Blackhole bh) {
        Object result = DynamicFinderHelper.findByFirstNameAndLastName(state.service, "First25", "Last25");
        bh.consume(result);
        return result;
    }

    @Benchmark
    public Object finder_findAllByLastNameOrAge(FinderState state, Blackhole bh) {
        Object result = DynamicFinderHelper.findAllByLastNameOrAge(state.service, "Last25", 30);
        bh.consume(result);
        return result;
    }

    // ========================================================================
    // Count finders
    // ========================================================================

    @Benchmark
    public Object finder_countByLastName(FinderState state, Blackhole bh) {
        Object result = DynamicFinderHelper.countByLastName(state.service, "Last25");
        bh.consume(result);
        return result;
    }

    @Benchmark
    public Object finder_countByAge(FinderState state, Blackhole bh) {
        Object result = DynamicFinderHelper.countByAge(state.service, 30);
        bh.consume(result);
        return result;
    }

    // ========================================================================
    // Mixed finder patterns (realistic usage)
    // ========================================================================

    @Benchmark
    public Object finder_mixedQueries(FinderState state, Blackhole bh) {
        int idx = state.random.nextInt(100);
        DynamicFinderHelper.mixedFinderOperations(state.service, idx, bh);
        return idx;
    }

    // ========================================================================
    // Java baseline (direct method calls)
    // ========================================================================

    @Benchmark
    public Object java_findByEmail(FinderState state, Blackhole bh) {
        Object result = state.service.javaFindByEmail("person42@example.com");
        bh.consume(result);
        return result;
    }

    @Benchmark
    public Object java_findAllByLastName(FinderState state, Blackhole bh) {
        Object result = state.service.javaFindAllByLastName("Last25");
        bh.consume(result);
        return result;
    }
}
