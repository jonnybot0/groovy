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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Benchmarks to understand threshold sensitivity for indy optimization.
 * <p>
 * This benchmark simulates different usage patterns that would benefit from
 * different threshold configurations:
 * <ul>
 *   <li>Web request pattern: Many short-lived objects with few calls each</li>
 *   <li>Batch processing pattern: Few objects with many calls each</li>
 *   <li>Mixed pattern: Combination of both</li>
 * </ul>
 * <p>
 * Run with different threshold values to find optimal settings:
 * <pre>
 * # Default threshold (10000)
 * ./gradlew -Pindy=true -PbenchInclude=ThresholdSensitivity :perf:jmh
 *
 * # Lower threshold (100)
 * ./gradlew -Pindy=true -PbenchInclude=ThresholdSensitivity :perf:jmh \
 *   --jvmArgs="-Dgroovy.indy.optimize.threshold=100"
 *
 * # Very low threshold (0 - immediate optimization)
 * ./gradlew -Pindy=true -PbenchInclude=ThresholdSensitivity :perf:jmh \
 *   --jvmArgs="-Dgroovy.indy.optimize.threshold=0 -Dgroovy.indy.fallback.threshold=0"
 * </pre>
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ThresholdSensitivityBench {

    // ========================================================================
    // Web Request Pattern: Many objects, few calls each
    // ========================================================================

    /**
     * Simulates web request handling: create object, call 1-5 methods, discard.
     * This pattern suffers most from high optimization thresholds.
     */
    @Benchmark
    public void webRequest_singleCall(Blackhole bh) {
        SimpleService svc = new SimpleService();
        bh.consume(svc.getName());
    }

    @Benchmark
    public void webRequest_fewCalls(Blackhole bh) {
        SimpleService svc = new SimpleService();
        bh.consume(svc.getName());
        bh.consume(svc.compute(1, 2));
        svc.doWork();
    }

    @Benchmark
    public void webRequest_typicalController(Blackhole bh) {
        // Simulate a typical controller action
        Person person = new Person();
        person.setFirstName("John");
        person.setLastName("Doe");

        // Access properties (common in view rendering)
        bh.consume(person.getFirstName());
        bh.consume(person.getLastName());
        bh.consume(person.getFullName());
    }

    // ========================================================================
    // Batch Processing Pattern: Few objects, many calls each
    // ========================================================================

    @State(Scope.Thread)
    public static class BatchState {
        SimpleService service;
        List<Person> people;

        @Setup(Level.Trial)
        public void setup() {
            service = new SimpleService();
            people = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                Person p = new Person();
                p.setFirstName("User" + i);
                p.setLastName("Batch");
                people.add(p);
            }
        }
    }

    /**
     * Batch processing: same method called many times on same object.
     * This pattern benefits from optimization even with high thresholds.
     */
    @Benchmark
    @OperationsPerInvocation(1000)
    public void batch_repeatMethodCall(BatchState state, Blackhole bh) {
        for (int i = 0; i < 1000; i++) {
            bh.consume(state.service.getName());
        }
    }

    /**
     * Batch: iterate over collection calling method on each.
     */
    @Benchmark
    @OperationsPerInvocation(1000)
    public void batch_collectionIteration(BatchState state, Blackhole bh) {
        for (Person p : state.people) {
            bh.consume(p.getFullName());
        }
    }

    // ========================================================================
    // Mixed Pattern: Combination of both
    // ========================================================================

    @State(Scope.Thread)
    public static class MixedState {
        List<Person> cachedPeople;
        int requestCount = 0;

        @Setup(Level.Trial)
        public void setup() {
            // Some cached objects (like in a session or application scope)
            cachedPeople = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                Person p = new Person();
                p.setFirstName("Cached" + i);
                p.setLastName("User");
                cachedPeople.add(p);
            }
        }
    }

    /**
     * Mixed: some cached objects + some new objects per request.
     */
    @Benchmark
    public void mixed_cachedAndNew(MixedState state, Blackhole bh) {
        // Access cached object (warm callsite)
        Person cached = state.cachedPeople.get(state.requestCount % 10);
        bh.consume(cached.getFullName());

        // Create new request-scoped object (cold callsite pattern)
        Person requestScoped = new Person();
        requestScoped.setFirstName("Request");
        requestScoped.setLastName(String.valueOf(state.requestCount++));
        bh.consume(requestScoped.getFullName());
    }

    // ========================================================================
    // Polymorphic Call Patterns (affected by fallback threshold)
    // ========================================================================

    @State(Scope.Thread)
    public static class PolymorphicState {
        Processable[] processors;

        @Setup(Level.Trial)
        public void setup() {
            // Mix of different implementation types
            processors = new Processable[] {
                new TypeA(), new TypeB(), new TypeC(), new TypeD(),
                new TypeE(), new TypeF(), new TypeG(), new TypeH()
            };
        }
    }

    /**
     * Polymorphic dispatch: same interface, different implementations.
     * This tests the inline cache and fallback behavior.
     */
    @Benchmark
    @OperationsPerInvocation(8)
    public void polymorphic_interfaceDispatch(PolymorphicState state, Blackhole bh) {
        for (Processable p : state.processors) {
            bh.consume(p.process());
        }
    }

    /**
     * Polymorphic with random access pattern (worst case for inline cache).
     */
    @Benchmark
    public void polymorphic_randomAccess(PolymorphicState state, Blackhole bh) {
        // Access in unpredictable order
        bh.consume(state.processors[3].process());
        bh.consume(state.processors[7].process());
        bh.consume(state.processors[1].process());
        bh.consume(state.processors[5].process());
        bh.consume(state.processors[0].process());
        bh.consume(state.processors[6].process());
        bh.consume(state.processors[2].process());
        bh.consume(state.processors[4].process());
    }

    // ========================================================================
    // Property Access Patterns (very common in Grails)
    // ========================================================================

    @State(Scope.Thread)
    public static class PropertyState {
        DomainLikeObject domain;

        @Setup(Level.Trial)
        public void setup() {
            domain = new DomainLikeObject();
            domain.setName("TestDomain");
            domain.setAge(25);
        }
    }

    /**
     * Property getter access.
     */
    @Benchmark
    public void property_getter(PropertyState state, Blackhole bh) {
        bh.consume(state.domain.getName());
        bh.consume(state.domain.getAge());
    }

    /**
     * Property setter access.
     */
    @Benchmark
    public void property_setter(PropertyState state, Blackhole bh) {
        state.domain.setName("Updated");
        state.domain.setAge(26);
        bh.consume(state.domain);
    }

    /**
     * Dynamic property access (uses getProperty/setProperty).
     */
    @Benchmark
    public void property_dynamic(PropertyState state, Blackhole bh) {
        state.domain.setProperty("dynamic_key", "value");
        bh.consume(state.domain.getProperty("dynamic_key"));
    }

    // ========================================================================
    // Java Baselines
    // ========================================================================

    public static class JavaPerson {
        private String firstName;
        private String lastName;

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public String getFullName() { return firstName + " " + lastName; }
    }

    @Benchmark
    public void java_webRequest(Blackhole bh) {
        JavaPerson person = new JavaPerson();
        person.setFirstName("John");
        person.setLastName("Doe");
        bh.consume(person.getFirstName());
        bh.consume(person.getLastName());
        bh.consume(person.getFullName());
    }

    @State(Scope.Thread)
    public static class JavaBatchState {
        List<JavaPerson> people;

        @Setup(Level.Trial)
        public void setup() {
            people = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                JavaPerson p = new JavaPerson();
                p.setFirstName("User" + i);
                p.setLastName("Batch");
                people.add(p);
            }
        }
    }

    @Benchmark
    @OperationsPerInvocation(1000)
    public void java_batchIteration(JavaBatchState state, Blackhole bh) {
        for (JavaPerson p : state.people) {
            bh.consume(p.getFullName());
        }
    }
}
