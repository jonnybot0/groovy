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

/**
 * Helper class for property access benchmarks.
 * Contains Groovy-specific operations like spread operator, collect, find, etc.
 */
class PropertyAccessHelper {

    /**
     * Spread operator to get all firstNames.
     * Equivalent to: people*.firstName
     */
    static List<String> spreadFirstName(List<PersonDomain> people) {
        people*.firstName
    }

    /**
     * Spread operator for computed property.
     */
    static List<String> spreadFullName(List<PersonDomain> people) {
        people*.fullName
    }

    /**
     * Collect with closure accessing property.
     */
    static List<String> collectFullNames(List<PersonDomain> people) {
        people.collect { it.fullName }
    }

    /**
     * Collect with multiple property accesses.
     */
    static List<Map> collectMultipleProps(List<PersonDomain> people) {
        people.collect { person ->
            [
                name: person.fullName,
                email: person.email,
                city: person.address?.city
            ]
        }
    }

    /**
     * Find by property value.
     */
    static PersonDomain findByEmail(List<PersonDomain> people, String email) {
        people.find { it.email == email }
    }

    /**
     * FindAll with property filter.
     */
    static List<PersonDomain> findAllActive(List<PersonDomain> people) {
        people.findAll { it.active }
    }

    /**
     * FindAll with nested property access.
     */
    static List<PersonDomain> findAllInState(List<PersonDomain> people, String state) {
        people.findAll { it.address?.state == state }
    }

    /**
     * Chained collection operations.
     */
    static List<String> chainedOperations(List<PersonDomain> people) {
        people.findAll { it.active }
              .collect { it.fullName }
              .sort()
    }

    /**
     * Group by property.
     */
    static Map<String, List<PersonDomain>> groupByState(List<PersonDomain> people) {
        people.groupBy { it.address?.state }
    }

    /**
     * Sum with property access.
     */
    static BigDecimal sumOrderTotals(List<OrderDomain> orders) {
        orders.sum { it.total } ?: 0.0
    }

    /**
     * Nested spread operator.
     */
    static List<String> nestedSpread(List<OrderDomain> orders) {
        orders*.customer*.fullName
    }

    /**
     * Deep property access with null safety.
     */
    static List<String> safeNestedAccess(List<OrderDomain> orders) {
        orders.collect { order ->
            order?.customer?.address?.city ?: 'Unknown'
        }
    }

    /**
     * Multiple levels of spread.
     */
    static List<List<String>> multiLevelSpread(List<OrderDomain> orders) {
        orders*.items*.product*.name
    }

    /**
     * Dynamic property access by name.
     */
    static List<Object> dynamicPropertyAccess(List<PersonDomain> people, String propName) {
        people.collect { it."$propName" }
    }

    /**
     * GString interpolation with property access.
     */
    static List<String> formatWithGString(List<PersonDomain> people) {
        people.collect { person ->
            "Name: ${person.fullName}, Email: ${person.email}, Location: ${person.address?.shortAddress ?: 'N/A'}"
        }
    }

    /**
     * Elvis operator for defaults.
     */
    static List<String> withElvisDefaults(List<PersonDomain> people) {
        people.collect { person ->
            person.email ?: "${person.firstName}.${person.lastName}@default.com".toLowerCase()
        }
    }

    /**
     * Simulate dynamic finder pattern.
     */
    static List<PersonDomain> findAllByLastNameLike(List<PersonDomain> people, String pattern) {
        people.findAll { it.lastName?.contains(pattern) }
    }

    /**
     * Simulate GORM criteria-like filtering.
     */
    static List<PersonDomain> filterByCriteria(List<PersonDomain> people, Map criteria) {
        people.findAll { person ->
            criteria.every { key, value ->
                if (value instanceof Closure) {
                    value(person."$key")
                } else {
                    person."$key" == value
                }
            }
        }
    }
}
