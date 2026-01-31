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
package org.apache.groovy.bench.dispatch

import org.openjdk.jmh.infra.Blackhole

/**
 * Helper for interface dispatch benchmarks.
 * Provides interfaces and implementations that exercise polymorphic dispatch.
 */
class InterfaceDispatchHelper {

    /**
     * Create a list of services with different implementations.
     */
    static List<InterfaceService> createServices(int count) {
        def result = []
        count.times { i ->
            switch (i % 4) {
                case 0: result << new ServiceImplA(name: "ServiceA$i", enabled: true); break
                case 1: result << new ServiceImplB(name: "ServiceB$i", enabled: i % 2 == 0); break
                case 2: result << new ServiceImplC(name: "ServiceC$i", enabled: true); break
                case 3: result << new ServiceImplD(name: "ServiceD$i", enabled: i % 3 == 0); break
            }
        }
        result
    }

    /**
     * Create a list of processors with different implementations.
     */
    static List<InterfaceProcessor> createProcessors(int count) {
        def result = []
        count.times { i ->
            switch (i % 3) {
                case 0: result << new ProcessorImplA(); break
                case 1: result << new ProcessorImplB(); break
                case 2: result << new ProcessorImplC(); break
            }
        }
        result
    }

    /**
     * Create a list of validators with different implementations.
     */
    static List<InterfaceValidator> createValidators(int count) {
        def result = []
        count.times { i ->
            switch (i % 2) {
                case 0: result << new ValidatorImplA(); break
                case 1: result << new ValidatorImplB(); break
            }
        }
        result
    }

    /**
     * Call execute() on all services.
     */
    static void callExecute(List<InterfaceService> services, Blackhole bh) {
        for (svc in services) {
            bh.consume(svc.execute("test"))
        }
    }

    /**
     * Call multiple methods on same interface.
     */
    static void callMultipleMethods(List<InterfaceService> services, Blackhole bh) {
        for (svc in services) {
            bh.consume(svc.execute("test"))
            bh.consume(svc.getName())
            bh.consume(svc.isEnabled())
        }
    }

    /**
     * Call process() on all processors.
     */
    static void callProcess(List<InterfaceProcessor> processors, Blackhole bh) {
        for (proc in processors) {
            bh.consume(proc.process([1, 2, 3]))
        }
    }

    /**
     * Call validate() on all validators.
     */
    static void callValidate(List<InterfaceValidator> validators, Blackhole bh) {
        for (val in validators) {
            bh.consume(val.validate("test"))
        }
    }

    /**
     * Typed dispatch - explicitly typed parameter.
     */
    static void typedDispatch(List<InterfaceService> services, Blackhole bh) {
        for (InterfaceService svc in services) {
            bh.consume(svc.execute("test"))
        }
    }

    /**
     * Untyped dispatch - def parameter.
     */
    static void untypedDispatch(List services, Blackhole bh) {
        for (def svc in services) {
            bh.consume(svc.execute("test"))
        }
    }

    /**
     * Call default method on interface.
     */
    static void callDefaultMethod(List<InterfaceService> services, Blackhole bh) {
        for (svc in services) {
            bh.consume(svc.getDescription())
        }
    }

    /**
     * Chained interface calls.
     */
    static void chainedInterfaceCalls(List<InterfaceService> services, List<InterfaceProcessor> processors, Blackhole bh) {
        services.eachWithIndex { svc, i ->
            def result = svc.execute("input")
            def processor = processors[i % processors.size()]
            bh.consume(processor.process([result]))
        }
    }

    /**
     * Collect results via interface.
     */
    static List collectResults(List<InterfaceService> services) {
        services.collect { it.execute("collect") }
    }

    /**
     * Filter by status via interface.
     */
    static List filterByStatus(List<InterfaceService> services) {
        services.findAll { it.isEnabled() }
    }
}

// ============================================================================
// Interface definitions
// ============================================================================

interface InterfaceService {
    String execute(String input)
    String getName()
    boolean isEnabled()

    // Default method (Java 8+)
    default String getDescription() {
        return "Service: " + getName()
    }
}

interface InterfaceProcessor {
    Object process(List items)
}

interface InterfaceValidator {
    boolean validate(Object input)
}

// ============================================================================
// Service implementations
// ============================================================================

class ServiceImplA implements InterfaceService {
    String name
    boolean enabled

    String execute(String input) { "A:$input" }
    String getName() { name }
    boolean isEnabled() { enabled }
}

class ServiceImplB implements InterfaceService {
    String name
    boolean enabled

    String execute(String input) { "B:${input.toUpperCase()}" }
    String getName() { name }
    boolean isEnabled() { enabled }
}

class ServiceImplC implements InterfaceService {
    String name
    boolean enabled

    String execute(String input) { "C:${input.reverse()}" }
    String getName() { name }
    boolean isEnabled() { enabled }
}

class ServiceImplD implements InterfaceService {
    String name
    boolean enabled

    String execute(String input) { "D:${input.length()}" }
    String getName() { name }
    boolean isEnabled() { enabled }
}

// ============================================================================
// Processor implementations
// ============================================================================

class ProcessorImplA implements InterfaceProcessor {
    Object process(List items) {
        items.collect { it.toString() }.join(',')
    }
}

class ProcessorImplB implements InterfaceProcessor {
    Object process(List items) {
        items.sum { it.toString().length() }
    }
}

class ProcessorImplC implements InterfaceProcessor {
    Object process(List items) {
        [count: items.size(), items: items]
    }
}

// ============================================================================
// Validator implementations
// ============================================================================

class ValidatorImplA implements InterfaceValidator {
    boolean validate(Object input) {
        input != null && input.toString().length() > 0
    }
}

class ValidatorImplB implements InterfaceValidator {
    boolean validate(Object input) {
        input != null && input.toString().matches(/\w+/)
    }
}
