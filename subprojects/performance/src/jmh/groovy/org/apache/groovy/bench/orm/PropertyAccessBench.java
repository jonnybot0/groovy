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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Benchmarks for measuring property access patterns common in GORM/Grails.
 * <p>
 * Property access is one of the most frequent operations in Grails applications,
 * especially in views (GSP) where domain object properties are rendered.
 * This benchmark tests:
 * <ul>
 *   <li>Simple property getters</li>
 *   <li>Computed properties (derived from other properties)</li>
 *   <li>Nested property access (object graphs)</li>
 *   <li>Collection property access (spread operator)</li>
 *   <li>Null-safe navigation</li>
 * </ul>
 * <p>
 * Run with: ./gradlew -Pindy=true -PbenchInclude=PropertyAccess :perf:jmh
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class PropertyAccessBench {

    // ========================================================================
    // State classes
    // ========================================================================

    @State(Scope.Thread)
    public static class SingleObjectState {
        PersonDomain person;
        OrderDomain order;

        @Setup(Level.Trial)
        public void setup() {
            person = DomainFactory.createPerson(1);
            person.setAddress(DomainFactory.createAddress(1));

            // Create products and order
            List<ProductDomain> products = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                products.add(DomainFactory.createProduct(i));
            }

            order = DomainFactory.createOrder(person, products, 1);
            person.getOrders().add(order);
        }
    }

    @State(Scope.Thread)
    public static class CollectionState {
        List<PersonDomain> people;
        List<OrderDomain> orders;
        List<ProductDomain> products;

        @Setup(Level.Trial)
        public void setup() {
            // Create products first
            products = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                products.add(DomainFactory.createProduct(i));
            }

            // Create people with addresses
            people = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                PersonDomain person = DomainFactory.createPerson(i);
                person.setAddress(DomainFactory.createAddress(i));
                people.add(person);
            }

            // Create orders
            orders = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                OrderDomain order = DomainFactory.createOrder(people.get(i % people.size()), products, i);
                orders.add(order);
                people.get(i % people.size()).getOrders().add(order);
            }
        }
    }

    @State(Scope.Thread)
    public static class HierarchyState {
        CategoryDomain rootCategory;
        List<CategoryDomain> leafCategories;

        @Setup(Level.Trial)
        public void setup() {
            // Create a category hierarchy: 3 levels deep, 4 children per level
            rootCategory = DomainFactory.createCategoryHierarchy(3, 4);

            // Collect leaf categories
            leafCategories = new ArrayList<>();
            collectLeaves(rootCategory, leafCategories);
        }

        private void collectLeaves(CategoryDomain category, List<CategoryDomain> leaves) {
            if (category.getChildren().isEmpty()) {
                leaves.add(category);
            } else {
                for (Object child : category.getChildren()) {
                    collectLeaves((CategoryDomain) child, leaves);
                }
            }
        }
    }

    // ========================================================================
    // Simple property access benchmarks
    // ========================================================================

    /**
     * Simple getter access.
     */
    @Benchmark
    public void simple_singleGetter(SingleObjectState state, Blackhole bh) {
        bh.consume(state.person.getFirstName());
    }

    /**
     * Multiple getter access on same object.
     */
    @Benchmark
    public void simple_multipleGetters(SingleObjectState state, Blackhole bh) {
        bh.consume(state.person.getFirstName());
        bh.consume(state.person.getLastName());
        bh.consume(state.person.getEmail());
        bh.consume(state.person.getAge());
        bh.consume(state.person.getActive());
    }

    /**
     * Computed property (calls other getters internally).
     */
    @Benchmark
    public void simple_computedProperty(SingleObjectState state, Blackhole bh) {
        bh.consume(state.person.getFullName());
    }

    /**
     * Computed property with conditional logic.
     */
    @Benchmark
    public void simple_computedWithLogic(SingleObjectState state, Blackhole bh) {
        bh.consume(state.person.getDisplayName());
    }

    // ========================================================================
    // Nested property access benchmarks
    // ========================================================================

    /**
     * One level of nesting: person.address.city
     */
    @Benchmark
    public void nested_oneLevel(SingleObjectState state, Blackhole bh) {
        bh.consume(state.person.getAddress().getCity());
    }

    /**
     * Multiple nested accesses.
     */
    @Benchmark
    public void nested_multipleAccess(SingleObjectState state, Blackhole bh) {
        AddressDomain addr = state.person.getAddress();
        bh.consume(addr.getStreet());
        bh.consume(addr.getCity());
        bh.consume(addr.getState());
        bh.consume(addr.getZipCode());
    }

    /**
     * Computed nested property.
     */
    @Benchmark
    public void nested_computedProperty(SingleObjectState state, Blackhole bh) {
        bh.consume(state.person.getAddress().getFullAddress());
    }

    /**
     * Deep nesting: order -> items -> product -> category
     */
    @Benchmark
    public void nested_deepAccess(SingleObjectState state, Blackhole bh) {
        for (Object item : state.order.getItems()) {
            OrderItemDomain orderItem = (OrderItemDomain) item;
            ProductDomain product = orderItem.getProduct();
            if (product != null && product.getCategory() != null) {
                bh.consume(product.getCategory().getName());
            }
        }
    }

    // ========================================================================
    // Collection property access benchmarks
    // ========================================================================

    /**
     * Iterate and access single property.
     */
    @Benchmark
    @OperationsPerInvocation(100)
    public void collection_iterateSingleProp(CollectionState state, Blackhole bh) {
        for (PersonDomain person : state.people) {
            bh.consume(person.getFirstName());
        }
    }

    /**
     * Iterate and access multiple properties.
     */
    @Benchmark
    @OperationsPerInvocation(100)
    public void collection_iterateMultipleProps(CollectionState state, Blackhole bh) {
        for (PersonDomain person : state.people) {
            bh.consume(person.getFirstName());
            bh.consume(person.getLastName());
            bh.consume(person.getEmail());
        }
    }

    /**
     * Spread operator access (Groovy specific).
     */
    @Benchmark
    public void collection_spreadOperator(CollectionState state, Blackhole bh) {
        List<?> result = PropertyAccessHelper.spreadFirstName(state.people);
        bh.consume(result);
    }

    /**
     * Collect with property access.
     */
    @Benchmark
    public void collection_collectProperty(CollectionState state, Blackhole bh) {
        List<?> result = PropertyAccessHelper.collectFullNames(state.people);
        bh.consume(result);
    }

    /**
     * Find with property comparison.
     */
    @Benchmark
    public void collection_findByProperty(CollectionState state, Blackhole bh) {
        Object result = PropertyAccessHelper.findByEmail(state.people, "user50@example.com");
        bh.consume(result);
    }

    /**
     * FindAll with property filter.
     */
    @Benchmark
    public void collection_findAllByProperty(CollectionState state, Blackhole bh) {
        List<?> result = PropertyAccessHelper.findAllActive(state.people);
        bh.consume(result);
    }

    // ========================================================================
    // View rendering simulation benchmarks
    // ========================================================================

    /**
     * Simulate rendering a single object view.
     */
    @Benchmark
    public void view_renderSingle(SingleObjectState state, Blackhole bh) {
        Map<?, ?> result = DomainOperations.renderPersonView(state.person);
        bh.consume(result);
    }

    /**
     * Simulate rendering a list view.
     */
    @Benchmark
    public void view_renderList(CollectionState state, Blackhole bh) {
        List<?> result = DomainOperations.renderPersonList(state.people);
        bh.consume(result);
    }

    /**
     * Simulate rendering order with nested data.
     */
    @Benchmark
    public void view_renderOrderDetails(SingleObjectState state, Blackhole bh) {
        List<?> result = DomainOperations.getOrderDetails(state.order);
        bh.consume(result);
    }

    /**
     * Simulate generating a report with aggregation.
     */
    @Benchmark
    public void view_generateReport(SingleObjectState state, Blackhole bh) {
        Map<?, ?> result = DomainOperations.generateCustomerReport(state.person);
        bh.consume(result);
    }

    // ========================================================================
    // Hierarchy traversal benchmarks
    // ========================================================================

    /**
     * Traverse up hierarchy (parent chain).
     */
    @Benchmark
    @OperationsPerInvocation(64)
    public void hierarchy_traverseUp(HierarchyState state, Blackhole bh) {
        for (CategoryDomain leaf : state.leafCategories) {
            List<?> path = DomainOperations.getCategoryPath(leaf);
            bh.consume(path);
        }
    }

    /**
     * Computed property with hierarchy traversal.
     */
    @Benchmark
    @OperationsPerInvocation(64)
    public void hierarchy_computedPath(HierarchyState state, Blackhole bh) {
        for (CategoryDomain leaf : state.leafCategories) {
            bh.consume(leaf.getFullPath());
        }
    }

    // ========================================================================
    // Java baseline benchmarks
    // ========================================================================

    public static class JavaPerson {
        private String firstName;
        private String lastName;
        private String email;
        private JavaAddress address;

        public String getFirstName() { return firstName; }
        public void setFirstName(String v) { firstName = v; }
        public String getLastName() { return lastName; }
        public void setLastName(String v) { lastName = v; }
        public String getEmail() { return email; }
        public void setEmail(String v) { email = v; }
        public JavaAddress getAddress() { return address; }
        public void setAddress(JavaAddress v) { address = v; }
        public String getFullName() { return firstName + " " + lastName; }
    }

    public static class JavaAddress {
        private String city;
        private String state;

        public String getCity() { return city; }
        public void setCity(String v) { city = v; }
        public String getState() { return state; }
        public void setState(String v) { state = v; }
    }

    @State(Scope.Thread)
    public static class JavaState {
        JavaPerson person;
        List<JavaPerson> people;

        @Setup(Level.Trial)
        public void setup() {
            person = new JavaPerson();
            person.setFirstName("John");
            person.setLastName("Doe");
            person.setEmail("john@example.com");
            JavaAddress addr = new JavaAddress();
            addr.setCity("NYC");
            addr.setState("NY");
            person.setAddress(addr);

            people = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                JavaPerson p = new JavaPerson();
                p.setFirstName("First" + i);
                p.setLastName("Last" + i);
                p.setEmail("user" + i + "@example.com");
                JavaAddress a = new JavaAddress();
                a.setCity("City" + i);
                a.setState("ST");
                p.setAddress(a);
                people.add(p);
            }
        }
    }

    @Benchmark
    public void java_singleGetter(JavaState state, Blackhole bh) {
        bh.consume(state.person.getFirstName());
    }

    @Benchmark
    public void java_multipleGetters(JavaState state, Blackhole bh) {
        bh.consume(state.person.getFirstName());
        bh.consume(state.person.getLastName());
        bh.consume(state.person.getEmail());
    }

    @Benchmark
    public void java_nestedAccess(JavaState state, Blackhole bh) {
        bh.consume(state.person.getAddress().getCity());
    }

    @Benchmark
    @OperationsPerInvocation(100)
    public void java_collectionIterate(JavaState state, Blackhole bh) {
        for (JavaPerson p : state.people) {
            bh.consume(p.getFirstName());
        }
    }

    @Benchmark
    public void java_collectionCollect(JavaState state, Blackhole bh) {
        List<String> result = new ArrayList<>();
        for (JavaPerson p : state.people) {
            result.add(p.getFullName());
        }
        bh.consume(result);
    }
}
