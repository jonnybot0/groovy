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
 * Benchmarks for interface-based polymorphic dispatch.
 * <p>
 * Tests scenarios where objects are accessed through interface types,
 * which is common in service layers and plugin architectures.
 * <p>
 * Run with: ./gradlew -Pindy=true -PbenchInclude=InterfaceDispatch :perf:jmh
 */
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(2)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class InterfaceDispatchBench {

    private static final int ITERATIONS = 1000;

    // ========================================================================
    // State classes
    // ========================================================================

    @State(Scope.Thread)
    public static class InterfaceState {
        List<InterfaceService> services;
        List<InterfaceProcessor> processors;
        List<InterfaceValidator> validators;

        @Setup(Level.Trial)
        public void setup() {
            services = InterfaceDispatchHelper.createServices(100);
            processors = InterfaceDispatchHelper.createProcessors(100);
            validators = InterfaceDispatchHelper.createValidators(100);
        }
    }

    // ========================================================================
    // Single interface dispatch
    // ========================================================================

    @Benchmark
    @OperationsPerInvocation(100)
    public void interface_singleMethod(InterfaceState state, Blackhole bh) {
        InterfaceDispatchHelper.callExecute(state.services, bh);
    }

    @Benchmark
    @OperationsPerInvocation(100)
    public void interface_multipleMethodsSameInterface(InterfaceState state, Blackhole bh) {
        InterfaceDispatchHelper.callMultipleMethods(state.services, bh);
    }

    // ========================================================================
    // Multiple interfaces
    // ========================================================================

    @Benchmark
    @OperationsPerInvocation(300)
    public void interface_multipleInterfaces(InterfaceState state, Blackhole bh) {
        InterfaceDispatchHelper.callExecute(state.services, bh);
        InterfaceDispatchHelper.callProcess(state.processors, bh);
        InterfaceDispatchHelper.callValidate(state.validators, bh);
    }

    // ========================================================================
    // Typed vs untyped dispatch
    // ========================================================================

    @Benchmark
    @OperationsPerInvocation(100)
    public void interface_typedDispatch(InterfaceState state, Blackhole bh) {
        InterfaceDispatchHelper.typedDispatch(state.services, bh);
    }

    @Benchmark
    @OperationsPerInvocation(100)
    public void interface_untypedDispatch(InterfaceState state, Blackhole bh) {
        InterfaceDispatchHelper.untypedDispatch(state.services, bh);
    }

    // ========================================================================
    // Interface with default methods (Java 8+)
    // ========================================================================

    @Benchmark
    @OperationsPerInvocation(100)
    public void interface_defaultMethod(InterfaceState state, Blackhole bh) {
        InterfaceDispatchHelper.callDefaultMethod(state.services, bh);
    }

    // ========================================================================
    // Chained interface calls
    // ========================================================================

    @Benchmark
    @OperationsPerInvocation(100)
    public void interface_chainedCalls(InterfaceState state, Blackhole bh) {
        InterfaceDispatchHelper.chainedInterfaceCalls(state.services, state.processors, bh);
    }

    // ========================================================================
    // Collection operations through interface
    // ========================================================================

    @Benchmark
    public Object interface_collectViaInterface(InterfaceState state, Blackhole bh) {
        Object result = InterfaceDispatchHelper.collectResults(state.services);
        bh.consume(result);
        return result;
    }

    @Benchmark
    public Object interface_filterViaInterface(InterfaceState state, Blackhole bh) {
        Object result = InterfaceDispatchHelper.filterByStatus(state.services);
        bh.consume(result);
        return result;
    }

    // ========================================================================
    // Java baseline (pure invokeinterface)
    // ========================================================================

    @Benchmark
    @OperationsPerInvocation(100)
    public void java_interfaceDispatch(InterfaceState state, Blackhole bh) {
        for (InterfaceService svc : state.services) {
            bh.consume(svc.execute("test"));
        }
    }

    @Benchmark
    @OperationsPerInvocation(100)
    public void java_multipleInterfaceMethods(InterfaceState state, Blackhole bh) {
        for (InterfaceService svc : state.services) {
            bh.consume(svc.execute("test"));
            bh.consume(svc.getName());
            bh.consume(svc.isEnabled());
        }
    }
}
