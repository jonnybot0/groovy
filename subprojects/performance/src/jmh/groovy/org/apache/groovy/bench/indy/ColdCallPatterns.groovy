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
package org.apache.groovy.bench.indy

/**
 * Groovy classes for cold call benchmarks.
 * These simulate patterns common in web applications where objects are
 * created, used briefly, and discarded - resulting in many "cold" callsites.
 */

/**
 * Simple service class with various method signatures.
 * Used to test cold method invocation overhead.
 */
class SimpleService {
    String getName() { "SimpleService" }
    int compute(int a, int b) { a + b }
    def process(Map params) { params.size() }
    void doWork() { /* no-op */ }
    static String staticMethod() { "static" }
}

/**
 * Factory that creates new instances for each call.
 * Simulates request-scoped object creation in web apps.
 */
class ServiceFactory {
    static SimpleService createService() {
        new SimpleService()
    }

    static Object createAndCall() {
        def svc = new SimpleService()
        svc.getName()
    }

    static Object createAndCallMultiple() {
        def svc = new SimpleService()
        svc.getName()
        svc.compute(1, 2)
        svc.process([a: 1, b: 2])
        svc.doWork()
    }
}

/**
 * Classes to test property access patterns (common in Grails domain objects).
 */
class DomainLikeObject {
    String name
    Integer age
    Date createdAt
    Map metadata = [:]

    def getProperty(String propName) {
        if (metadata.containsKey(propName)) {
            return metadata[propName]
        }
        return super.getProperty(propName)
    }

    void setProperty(String propName, Object value) {
        if (propName.startsWith('dynamic_')) {
            metadata[propName] = value
        } else {
            super.setProperty(propName, value)
        }
    }
}

/**
 * Multiple domain classes to simulate polymorphic scenarios.
 */
class Person {
    String firstName
    String lastName
    String getFullName() { "$firstName $lastName" }
}

class Employee extends Person {
    String department
    BigDecimal salary
}

class Customer extends Person {
    String accountNumber
    Date memberSince
}

class Vendor extends Person {
    String companyName
    List<String> products = []
}

/**
 * Interface-based dispatch testing.
 */
interface Processable {
    Object process()
    String getType()
}

class TypeA implements Processable {
    Object process() { "processed-A" }
    String getType() { "A" }
}

class TypeB implements Processable {
    Object process() { "processed-B" }
    String getType() { "B" }
}

class TypeC implements Processable {
    Object process() { "processed-C" }
    String getType() { "C" }
}

class TypeD implements Processable {
    Object process() { "processed-D" }
    String getType() { "D" }
}

class TypeE implements Processable {
    Object process() { "processed-E" }
    String getType() { "E" }
}

class TypeF implements Processable {
    Object process() { "processed-F" }
    String getType() { "F" }
}

class TypeG implements Processable {
    Object process() { "processed-G" }
    String getType() { "G" }
}

class TypeH implements Processable {
    Object process() { "processed-H" }
    String getType() { "H" }
}

/**
 * Utility class for cold call operations.
 */
class ColdCallOperations {
    /**
     * Simulates a typical web request: create object, access properties, call methods.
     */
    static Object simulateRequest(int id) {
        def person = new Person(firstName: "User", lastName: "Number$id")
        def result = person.getFullName()
        return result
    }

    /**
     * Call a method N times to test warmup behavior.
     */
    static void callNTimes(Object target, String methodName, int n) {
        for (int i = 0; i < n; i++) {
            target."$methodName"()
        }
    }

    /**
     * Access a property N times.
     */
    static void accessPropertyNTimes(Object target, String propName, int n) {
        for (int i = 0; i < n; i++) {
            target."$propName"
        }
    }

    /**
     * Create N different objects and call a method on each (all cold calls).
     */
    static void coldCallsOnNewObjects(int n) {
        for (int i = 0; i < n; i++) {
            def svc = new SimpleService()
            svc.getName()
        }
    }

    /**
     * Simulate GString interpolation which involves method calls.
     */
    static String gstringInterpolation(Person p) {
        "Hello, ${p.getFullName()}! Welcome back."
    }

    /**
     * Spread operator on collection - common GORM pattern.
     */
    static List<String> spreadOperator(List<Person> people) {
        people*.getFullName()
    }

    /**
     * Collection operations - very common in Grails.
     */
    static def collectionOperations(List<Person> people) {
        people.findAll { it.firstName.startsWith('A') }
              .collect { it.getFullName() }
              .join(', ')
    }
}
