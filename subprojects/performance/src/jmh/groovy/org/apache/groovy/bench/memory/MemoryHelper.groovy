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
package org.apache.groovy.bench.memory

import org.openjdk.jmh.infra.Blackhole

/**
 * Groovy helper classes for memory pressure benchmarks.
 * These classes exercise callsite creation and method handle caching.
 */

/**
 * Simple service class for memory testing.
 */
class MemoryTestService {
    String name = "TestService"
    int counter = 0

    String getName() { name }
    int getCounter() { counter }
    void increment() { counter++ }
    int compute(int a, int b) { a + b }
    String process(String input) { input.toUpperCase() }
    def transform(Map data) { data.collect { k, v -> "$k=$v" }.join(', ') }
}

/**
 * Multiple receiver types for polymorphic dispatch testing.
 */
interface MemoryReceiver {
    String receive()
    int getValue()
}

class MemoryReceiverA implements MemoryReceiver {
    String receive() { "A" }
    int getValue() { 1 }
}

class MemoryReceiverB implements MemoryReceiver {
    String receive() { "B" }
    int getValue() { 2 }
}

class MemoryReceiverC implements MemoryReceiver {
    String receive() { "C" }
    int getValue() { 3 }
}

class MemoryReceiverD implements MemoryReceiver {
    String receive() { "D" }
    int getValue() { 4 }
}

class MemoryReceiverE implements MemoryReceiver {
    String receive() { "E" }
    int getValue() { 5 }
}

class MemoryReceiverF implements MemoryReceiver {
    String receive() { "F" }
    int getValue() { 6 }
}

class MemoryReceiverG implements MemoryReceiver {
    String receive() { "G" }
    int getValue() { 7 }
}

class MemoryReceiverH implements MemoryReceiver {
    String receive() { "H" }
    int getValue() { 8 }
}

/**
 * Domain-like object for session simulation.
 */
class SessionData {
    String sessionId
    String userId
    Map<String, Object> attributes = [:]
    long createdAt
    long lastAccessed

    void setAttribute(String key, Object value) {
        attributes[key] = value
        lastAccessed = System.currentTimeMillis()
    }

    Object getAttribute(String key) {
        lastAccessed = System.currentTimeMillis()
        attributes[key]
    }

    boolean isExpired(long timeout) {
        System.currentTimeMillis() - lastAccessed > timeout
    }

    String getSummary() {
        "Session[$sessionId]: user=$userId, attrs=${attributes.size()}"
    }
}

/**
 * Helper methods for memory benchmarks.
 * All methods use Groovy's dynamic dispatch to exercise callsite creation.
 */
class MemoryHelper {

    /**
     * Create N objects and call one method on each (cold callsites).
     * Each iteration creates new objects, triggering callsite population.
     */
    static void createAndCallMany(int count, Blackhole bh) {
        for (int i = 0; i < count; i++) {
            def svc = new MemoryTestService()
            bh.consume(svc.getName())
        }
    }

    /**
     * Create N objects and call multiple methods on each (cache filling).
     * This exercises the 4-entry LRU cache in CacheableCallSite.
     */
    static void createAndCallMultiple(int count, Blackhole bh) {
        for (int i = 0; i < count; i++) {
            def svc = new MemoryTestService()
            bh.consume(svc.getName())
            bh.consume(svc.getCounter())
            svc.increment()
            bh.consume(svc.compute(i, i + 1))
        }
    }

    /**
     * Polymorphic dispatch to mixed types (cache invalidation).
     * Tests the cost of cache churn with multiple receiver types.
     */
    static void polymorphicDispatch(List receivers, Blackhole bh) {
        for (receiver in receivers) {
            bh.consume(receiver.receive())
            bh.consume(receiver.getValue())
        }
    }

    /**
     * Exercise spread operator overhead.
     * The spread operator creates callsites for each element.
     */
    static List spreadOperatorOverhead(List objects, Blackhole bh) {
        def result = objects*.receive()
        bh.consume(result)
        result
    }

    /**
     * Exercise closure allocation and invocation overhead.
     * Closures capture the enclosing scope and create additional objects.
     */
    static void closureOverhead(List objects, Blackhole bh) {
        def result = objects.collect { it.receive() }
            .findAll { it != null }
            .take(10)
        bh.consume(result)
    }

    /**
     * Simulate a single request cycle: create session, access data, discard.
     * This is the pattern that exercises cold callsite performance.
     */
    static Object simulateRequest(String requestId, Blackhole bh) {
        def session = new SessionData(
            sessionId: requestId,
            userId: "user_${requestId.hashCode() % 1000}",
            createdAt: System.currentTimeMillis(),
            lastAccessed: System.currentTimeMillis()
        )
        session.setAttribute("requestId", requestId)
        session.setAttribute("timestamp", System.currentTimeMillis())
        def summary = session.getSummary()
        bh.consume(session)
        bh.consume(summary)
        summary
    }

    /**
     * Create a mixed list of receiver types for polymorphic testing.
     */
    static List<MemoryReceiver> createMixedReceivers(int count) {
        def types = [
            MemoryReceiverA, MemoryReceiverB, MemoryReceiverC, MemoryReceiverD,
            MemoryReceiverE, MemoryReceiverF, MemoryReceiverG, MemoryReceiverH
        ]
        def receivers = []
        for (int i = 0; i < count; i++) {
            receivers << types[i % types.size()].newInstance()
        }
        receivers
    }

    /**
     * Create receivers with only N distinct types.
     */
    static List<MemoryReceiver> createReceiversWithNTypes(int count, int numTypes) {
        def types = [
            MemoryReceiverA, MemoryReceiverB, MemoryReceiverC, MemoryReceiverD,
            MemoryReceiverE, MemoryReceiverF, MemoryReceiverG, MemoryReceiverH
        ].take(numTypes)
        def receivers = []
        for (int i = 0; i < count; i++) {
            receivers << types[i % types.size()].newInstance()
        }
        receivers
    }

    /**
     * Call unique method names dynamically (exercises different callsites).
     */
    static void callUniqueMethods(Object target, List<String> methodNames, Blackhole bh) {
        for (name in methodNames) {
            try {
                bh.consume(target."$name"())
            } catch (MissingMethodException ignored) {
                // Expected for non-existent methods
            }
        }
    }

    /**
     * Access properties dynamically on multiple objects.
     */
    static void accessProperties(List objects, String propertyName, Blackhole bh) {
        for (obj in objects) {
            bh.consume(obj."$propertyName")
        }
    }
}
