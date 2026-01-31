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
package org.apache.groovy.bench.orm

import org.openjdk.jmh.infra.Blackhole

/**
 * Helper for invoking dynamic finder methods from Java benchmarks.
 * Groovy code can call methodMissing-based dynamic methods directly.
 */
class DynamicFinderHelper {

    // ========================================================================
    // findByX methods
    // ========================================================================

    static Object findByEmail(DynamicFinderService service, String email) {
        service.findByEmail(email)
    }

    static Object findByLastName(DynamicFinderService service, String lastName) {
        service.findByLastName(lastName)
    }

    static Object findByAge(DynamicFinderService service, int age) {
        service.findByAge(age)
    }

    static Object findByFirstNameAndLastName(DynamicFinderService service, String firstName, String lastName) {
        service.findByFirstNameAndLastName(firstName, lastName)
    }

    // ========================================================================
    // findAllByX methods
    // ========================================================================

    static Object findAllByLastName(DynamicFinderService service, String lastName) {
        service.findAllByLastName(lastName)
    }

    static Object findAllByAge(DynamicFinderService service, int age) {
        service.findAllByAge(age)
    }

    static Object findAllByLastNameOrAge(DynamicFinderService service, String lastName, int age) {
        service.findAllByLastNameOrAge(lastName, age)
    }

    // ========================================================================
    // countByX methods
    // ========================================================================

    static Object countByLastName(DynamicFinderService service, String lastName) {
        service.countByLastName(lastName)
    }

    static Object countByAge(DynamicFinderService service, int age) {
        service.countByAge(age)
    }

    // ========================================================================
    // Mixed operations for benchmarks
    // ========================================================================

    static void mixedFinderOperations(DynamicFinderService service, int idx, Blackhole bh) {
        bh.consume(service.findByEmail("person" + idx + "@example.com"))
        bh.consume(service.findAllByLastName("Last" + (idx % 100)))
        bh.consume(service.countByAge(idx % 50))
    }

    // ========================================================================
    // Batch operations
    // ========================================================================

    static void batchFindByEmail(DynamicFinderService service, int count, Blackhole bh) {
        for (int i = 0; i < count; i++) {
            bh.consume(service.findByEmail("person" + i + "@example.com"))
        }
    }

    static void batchFindAllByLastName(DynamicFinderService service, int count, Blackhole bh) {
        for (int i = 0; i < count; i++) {
            bh.consume(service.findAllByLastName("Last" + (i % 100)))
        }
    }

    static void batchCountByAge(DynamicFinderService service, int count, Blackhole bh) {
        for (int i = 0; i < count; i++) {
            bh.consume(service.countByAge(i % 50))
        }
    }
}
