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
import java.util.stream.Collectors

/**
 * Collection operations helper for benchmarks.
 */
class CollectionOps {

    List<CollectionDomain> createDomains(int count) {
        def random = new Random(42)
        def categories = ['A', 'B', 'C', 'D', 'E']
        def statuses = ['ACTIVE', 'INACTIVE', 'PENDING', 'ARCHIVED']

        (0..<count).collect { i ->
            new CollectionDomain(
                id: i,
                name: "Domain $i",
                category: categories[random.nextInt(categories.size())],
                status: statuses[random.nextInt(statuses.size())],
                value: random.nextInt(10000),
                tags: (0..<(random.nextInt(5) + 1)).collect { "tag$it" }
            )
        }
    }

    // ========================================================================
    // Basic iteration
    // ========================================================================

    void doEach(List<CollectionDomain> domains, Blackhole bh) {
        domains.each { domain ->
            bh.consume(domain.name)
            bh.consume(domain.value)
        }
    }

    void doEachWithIndex(List<CollectionDomain> domains, Blackhole bh) {
        domains.eachWithIndex { domain, idx ->
            bh.consume(domain.name)
            bh.consume(idx)
        }
    }

    // ========================================================================
    // Transformation
    // ========================================================================

    List doCollect(List<CollectionDomain> domains) {
        domains.collect { it.name }
    }

    List doCollectNested(List<CollectionDomain> domains) {
        domains.collect { domain ->
            [
                id: domain.id,
                name: domain.name,
                upperName: domain.name.toUpperCase(),
                valueDoubled: domain.value * 2
            ]
        }
    }

    List doCollectMany(List<CollectionDomain> domains) {
        domains.collectMany { it.tags }
    }

    // ========================================================================
    // Filtering
    // ========================================================================

    List doFindAll(List<CollectionDomain> domains) {
        domains.findAll { it.status == 'ACTIVE' && it.value > 5000 }
    }

    CollectionDomain doFind(List<CollectionDomain> domains) {
        domains.find { it.value > 9000 }
    }

    List doGrep(List<CollectionDomain> domains) {
        domains.grep { it.category == 'A' }
    }

    // ========================================================================
    // Aggregation
    // ========================================================================

    Map doGroupBy(List<CollectionDomain> domains) {
        domains.groupBy { it.category }
    }

    Map doCountBy(List<CollectionDomain> domains) {
        domains.countBy { it.status }
    }

    def doSum(List<CollectionDomain> domains) {
        domains.sum { it.value }
    }

    CollectionDomain doMax(List<CollectionDomain> domains) {
        domains.max { it.value }
    }

    // ========================================================================
    // Chained operations
    // ========================================================================

    List doChainedOperations(List<CollectionDomain> domains) {
        domains.findAll { it.status == 'ACTIVE' }
               .collect { it.name }
               .unique()
               .sort()
    }

    Map doComplexChain(List<CollectionDomain> domains) {
        domains.findAll { it.value > 1000 }
               .groupBy { it.category }
               .collectEntries { category, items ->
                   [category, [
                       count: items.size(),
                       total: items.sum { it.value },
                       avg: items.sum { it.value } / items.size(),
                       names: items*.name
                   ]]
               }
    }

    // ========================================================================
    // Sorting
    // ========================================================================

    List doSort(List<CollectionDomain> domains) {
        domains.sort { it.name }
    }

    List doSortBy(List<CollectionDomain> domains) {
        domains.sort { a, b -> b.value <=> a.value }
    }

    // ========================================================================
    // Java Stream baseline
    // ========================================================================

    List<String> javaStreamCollect(List<CollectionDomain> domains) {
        domains.stream()
               .map { it.name }
               .collect(Collectors.toList())
    }

    List<CollectionDomain> javaStreamFilter(List<CollectionDomain> domains) {
        domains.stream()
               .filter { it.status == 'ACTIVE' && it.value > 5000 }
               .collect(Collectors.toList())
    }

    List<String> javaStreamChained(List<CollectionDomain> domains) {
        domains.stream()
               .filter { it.status == 'ACTIVE' }
               .map { it.name }
               .distinct()
               .sorted()
               .collect(Collectors.toList())
    }
}

/**
 * Domain class for collection operations.
 */
class CollectionDomain {
    long id
    String name
    String category
    String status
    int value
    List<String> tags = []
}
