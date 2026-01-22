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
 * Benchmarks for measuring cold call overhead in Groovy's invokedynamic implementation.
 * <p>
 * Cold calls are method invocations that happen before the callsite has been optimized.
 * This is particularly important for web applications where objects are created per-request
 * and may only have their methods called a few times before being discarded.
 * <p>
 * Key areas tested:
 * <ul>
 *   <li>First invocation cost (callsite bootstrap)</li>
 *   <li>Create-and-call patterns (new object + immediate method call)</li>
 *   <li>Property access on new objects</li>
 *   <li>Spread operator overhead</li>
 *   <li>Collection operations</li>
 * </ul>
 * <p>
 * Run with: ./gradlew -Pindy=true -PbenchInclude=ColdCallBench :perf:jmh
 * Compare: Run once with -Pindy=true and once without to see the difference.
 */
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(3)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class ColdCallBench {

    // ========================================================================
    // State classes
    // ========================================================================

    @State(Scope.Thread)
    public static class ServiceState {
        SimpleService service;
        Person person;
        List<Person> people;

        @Setup(Level.Invocation)
        public void setupPerInvocation() {
            // Create fresh objects for each invocation to simulate cold calls
            service = new SimpleService();
            person = new Person();
            person.setFirstName("John");
            person.setLastName("Doe");
        }

        @Setup(Level.Trial)
        public void setupTrial() {
            // Create a list of people for collection operations
            people = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                Person p = new Person();
                p.setFirstName(i % 2 == 0 ? "Alice" : "Bob");
                p.setLastName("User" + i);
                people.add(p);
            }
        }
    }

    @State(Scope.Thread)
    public static class WarmServiceState {
        SimpleService service;
        Person person;

        @Setup(Level.Trial)
        public void setup() {
            // Create objects once and reuse - tests "warm" callsite performance
            service = new SimpleService();
            person = new Person();
            person.setFirstName("John");
            person.setLastName("Doe");

            // Warm up the callsites
            for (int i = 0; i < 20000; i++) {
                service.getName();
                service.compute(1, 2);
                person.getFullName();
            }
        }
    }

    // ========================================================================
    // Cold call benchmarks - new object each invocation
    // ========================================================================

    /**
     * Baseline: Create object only (no method call).
     */
    @Benchmark
    public Object cold_01_createObjectOnly(Blackhole bh) {
        SimpleService svc = new SimpleService();
        bh.consume(svc);
        return svc;
    }

    /**
     * Cold call: Create object and call one method.
     * This is the most common pattern in web apps.
     */
    @Benchmark
    public Object cold_02_createAndCallOne(ServiceState state, Blackhole bh) {
        Object result = state.service.getName();
        bh.consume(result);
        return result;
    }

    /**
     * Cold call: Create object and call multiple methods.
     */
    @Benchmark
    public Object cold_03_createAndCallMultiple(ServiceState state, Blackhole bh) {
        Object r1 = state.service.getName();
        Object r2 = state.service.compute(1, 2);
        state.service.doWork();
        bh.consume(r1);
        bh.consume(r2);
        return r2;
    }

    /**
     * Cold call: Property access on new object.
     */
    @Benchmark
    public Object cold_04_propertyAccess(ServiceState state, Blackhole bh) {
        Object result = state.person.getFullName();
        bh.consume(result);
        return result;
    }

    /**
     * Cold call via factory pattern (common in frameworks).
     */
    @Benchmark
    public Object cold_05_factoryCreateAndCall(Blackhole bh) {
        Object result = ServiceFactory.createAndCall();
        bh.consume(result);
        return result;
    }

    /**
     * Cold call via factory with multiple method calls.
     */
    @Benchmark
    public Object cold_06_factoryCreateAndCallMultiple(Blackhole bh) {
        Object result = ServiceFactory.createAndCallMultiple();
        bh.consume(result);
        return result;
    }

    // ========================================================================
    // Warm call benchmarks - same object, reused callsite
    // ========================================================================

    /**
     * Warm call: Same object, callsite should be optimized.
     */
    @Benchmark
    public Object warm_01_singleMethod(WarmServiceState state, Blackhole bh) {
        Object result = state.service.getName();
        bh.consume(result);
        return result;
    }

    /**
     * Warm call: Multiple methods on warmed object.
     */
    @Benchmark
    public Object warm_02_multipleMethods(WarmServiceState state, Blackhole bh) {
        Object r1 = state.service.getName();
        Object r2 = state.service.compute(1, 2);
        state.service.doWork();
        bh.consume(r1);
        bh.consume(r2);
        return r2;
    }

    /**
     * Warm call: Property access.
     */
    @Benchmark
    public Object warm_03_propertyAccess(WarmServiceState state, Blackhole bh) {
        Object result = state.person.getFullName();
        bh.consume(result);
        return result;
    }

    // ========================================================================
    // Collection operation benchmarks
    // ========================================================================

    /**
     * Spread operator on collection of objects.
     * Common GORM pattern: domainObjects*.propertyName
     */
    @Benchmark
    public Object collection_01_spreadOperator(ServiceState state, Blackhole bh) {
        List<String> result = ColdCallOperations.spreadOperator(state.people);
        bh.consume(result);
        return result;
    }

    /**
     * Collection operations: findAll + collect + join.
     * Very common in Grails controllers and services.
     */
    @Benchmark
    public Object collection_02_chainedOperations(ServiceState state, Blackhole bh) {
        Object result = ColdCallOperations.collectionOperations(state.people);
        bh.consume(result);
        return result;
    }

    // ========================================================================
    // GString interpolation benchmark
    // ========================================================================

    /**
     * GString with method call interpolation.
     */
    @Benchmark
    public Object gstring_01_interpolation(ServiceState state, Blackhole bh) {
        String result = ColdCallOperations.gstringInterpolation(state.person);
        bh.consume(result);
        return result;
    }

    // ========================================================================
    // Static method benchmarks (for comparison)
    // ========================================================================

    /**
     * Static method call (should have less overhead).
     */
    @Benchmark
    public Object static_01_methodCall(Blackhole bh) {
        Object result = SimpleService.staticMethod();
        bh.consume(result);
        return result;
    }

    // ========================================================================
    // Java equivalent benchmarks for comparison
    // ========================================================================

    /**
     * Java equivalent: create and call.
     */
    @Benchmark
    public Object java_01_createAndCall(Blackhole bh) {
        JavaService svc = new JavaService();
        Object result = svc.getName();
        bh.consume(result);
        return result;
    }

    /**
     * Java equivalent: multiple calls.
     */
    @Benchmark
    public Object java_02_createAndCallMultiple(Blackhole bh) {
        JavaService svc = new JavaService();
        Object r1 = svc.getName();
        int r2 = svc.compute(1, 2);
        svc.doWork();
        bh.consume(r1);
        bh.consume(r2);
        return r2;
    }

    // Java equivalent class
    public static class JavaService {
        public String getName() { return "JavaService"; }
        public int compute(int a, int b) { return a + b; }
        public void doWork() { /* no-op */ }
    }
}
