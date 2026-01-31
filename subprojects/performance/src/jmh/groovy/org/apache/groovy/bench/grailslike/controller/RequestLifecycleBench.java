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
package org.apache.groovy.bench.grailslike.controller;

import org.apache.groovy.bench.grailslike.model.*;
import org.apache.groovy.bench.grailslike.service.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Benchmarks simulating full HTTP request lifecycle in a Grails-like application.
 * <p>
 * This exercises the complete call path:
 * <ol>
 *   <li>Controller receives request (params parsing)</li>
 *   <li>Service layer invocation (transactional context)</li>
 *   <li>Domain object manipulation (property access, validation)</li>
 *   <li>Model building for view</li>
 * </ol>
 * <p>
 * Run with: ./gradlew -Pindy=true -PbenchInclude=RequestLifecycle :perf:jmh
 */
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(2)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class RequestLifecycleBench {

    // ========================================================================
    // State classes
    // ========================================================================

    @State(Scope.Thread)
    public static class ApplicationState {
        ControllerSimulator controller;
        DynamicService dynamicService;
        TransactionalSimulator transactionalService;
        Random random;

        @Setup(Level.Trial)
        public void setup() {
            random = new Random(42);

            // Initialize services
            dynamicService = new DynamicService();
            transactionalService = new TransactionalSimulator();
            transactionalService.setDynamicService(dynamicService);

            // Initialize controller
            controller = new ControllerSimulator();
            controller.setDynamicService(dynamicService);
            controller.setTransactionalService(transactionalService);

            // Seed data
            seedTestData();
        }

        private void seedTestData() {
            // Create 100 people
            for (int i = 0; i < 100; i++) {
                Person person = new Person();
                person.setFirstName("First" + i);
                person.setLastName("Last" + i);
                person.setEmail("person" + i + "@example.com");
                person.setPhone("555-" + String.format("%04d", i));
                person.save();
                dynamicService.getPeople().add(person);
            }

            // Create orders for some people
            List<Person> people = dynamicService.getPeople();
            for (int i = 0; i < 50; i++) {
                Person customer = people.get(random.nextInt(people.size()));

                Order order = new Order();
                order.setOrderNumber(Order.generateOrderNumber());
                order.setOrderDate(new Date());
                order.setCustomer(customer);

                // Add items
                int itemCount = random.nextInt(5) + 1;
                for (int j = 0; j < itemCount; j++) {
                    OrderItem item = ProductCatalog.randomItem(random);
                    order.addToItems(item);
                }

                order.save();
                dynamicService.getOrders().add(order);
                customer.addToOrders(order);
            }
        }

        @Setup(Level.Invocation)
        public void resetController() {
            controller.reset();
        }
    }

    // ========================================================================
    // List/Index Action Benchmarks
    // ========================================================================

    /**
     * List people action - pagination, sorting.
     */
    @Benchmark
    public Object request_listPeople(ApplicationState state, Blackhole bh) {
        state.controller.getParams().put("max", "20");
        state.controller.getParams().put("offset", "0");
        state.controller.getParams().put("sort", "lastName");
        state.controller.getParams().put("order", "asc");

        Map result = state.controller.listPeople();
        bh.consume(result);
        return result;
    }

    /**
     * List orders with status filter.
     */
    @Benchmark
    public Object request_listOrders(ApplicationState state, Blackhole bh) {
        state.controller.getParams().put("status", "PENDING");

        Map result = state.controller.listOrders();
        bh.consume(result);
        return result;
    }

    // ========================================================================
    // Show/Detail Action Benchmarks
    // ========================================================================

    /**
     * Show person detail - loads related orders.
     */
    @Benchmark
    public Object request_showPerson(ApplicationState state, Blackhole bh) {
        List<Person> people = state.dynamicService.getPeople();
        Person person = people.get(state.random.nextInt(people.size()));

        state.controller.getParams().put("id", person.getId().toString());

        Map result = state.controller.showPerson();
        bh.consume(result);
        return result;
    }

    /**
     * Show order detail - heavy property access.
     */
    @Benchmark
    public Object request_showOrder(ApplicationState state, Blackhole bh) {
        List<Order> orders = state.dynamicService.getOrders();
        if (orders.isEmpty()) return null;

        Order order = orders.get(state.random.nextInt(orders.size()));
        state.controller.getParams().put("id", order.getId().toString());

        Map result = state.controller.showOrder();
        bh.consume(result);
        return result;
    }

    // ========================================================================
    // Create/Update Action Benchmarks
    // ========================================================================

    /**
     * Create person - form submission, validation, save.
     */
    @Benchmark
    public Object request_createPerson(ApplicationState state, Blackhole bh) {
        int i = state.random.nextInt(10000);
        state.controller.getParams().put("firstName", "New" + i);
        state.controller.getParams().put("lastName", "Person" + i);
        state.controller.getParams().put("email", "new" + i + "@example.com");
        state.controller.getParams().put("phone", "555-" + String.format("%04d", i));

        Map result = state.controller.createPerson();
        bh.consume(result);
        return result;
    }

    /**
     * Create order with items - complex nested save.
     */
    @Benchmark
    public Object request_createOrder(ApplicationState state, Blackhole bh) {
        List<Person> people = state.dynamicService.getPeople();
        Person customer = people.get(state.random.nextInt(people.size()));

        state.controller.getParams().put("customerId", customer.getId().toString());
        state.controller.getParams().put("itemCount", "3");

        // Add items to params
        for (int i = 0; i < 3; i++) {
            state.controller.getParams().put("items[" + i + "].productCode", "PROD-" + i);
            state.controller.getParams().put("items[" + i + "].productName", "Product " + i);
            state.controller.getParams().put("items[" + i + "].quantity", String.valueOf(state.random.nextInt(5) + 1));
            state.controller.getParams().put("items[" + i + "].unitPrice", "99.99");
        }

        Map result = state.controller.createOrder();
        bh.consume(result);
        return result;
    }

    /**
     * Update person - edit form submission.
     */
    @Benchmark
    public Object request_updatePerson(ApplicationState state, Blackhole bh) {
        List<Person> people = state.dynamicService.getPeople();
        Person person = people.get(state.random.nextInt(people.size()));

        state.controller.getParams().put("id", person.getId().toString());
        state.controller.getParams().put("firstName", person.getFirstName() + "-updated");
        state.controller.getParams().put("lastName", person.getLastName());
        state.controller.getParams().put("email", person.getEmail());

        Map result = state.controller.updatePerson();
        bh.consume(result);
        return result;
    }

    // ========================================================================
    // Dashboard/Report Benchmarks
    // ========================================================================

    /**
     * Dashboard - aggregates, top N queries, recent items.
     */
    @Benchmark
    public Object request_dashboard(ApplicationState state, Blackhole bh) {
        Map result = state.controller.dashboard();
        bh.consume(result);
        return result;
    }

    /**
     * Export orders - large data transformation.
     */
    @Benchmark
    public Object request_exportOrders(ApplicationState state, Blackhole bh) {
        Map result = state.controller.exportOrders();
        bh.consume(result);
        return result;
    }

    // ========================================================================
    // AJAX/API Benchmarks
    // ========================================================================

    /**
     * JSON order details - nested object serialization.
     */
    @Benchmark
    public Object request_jsonOrderDetails(ApplicationState state, Blackhole bh) {
        List<Order> orders = state.dynamicService.getOrders();
        if (orders.isEmpty()) return null;

        Order order = orders.get(state.random.nextInt(orders.size()));
        state.controller.getParams().put("id", order.getId().toString());

        Map result = state.controller.jsonOrderDetails();
        bh.consume(result);
        return result;
    }

    /**
     * Person search - autocomplete pattern.
     */
    @Benchmark
    public Object request_searchPeople(ApplicationState state, Blackhole bh) {
        state.controller.getParams().put("term", "First");
        state.controller.getParams().put("max", "10");

        List result = state.controller.searchPeople();
        bh.consume(result);
        return result;
    }

    // ========================================================================
    // Full Request Cycle Benchmarks
    // ========================================================================

    /**
     * Complete CRUD cycle: list -> show -> edit -> save.
     */
    @Benchmark
    public Object request_crudCycle(ApplicationState state, Blackhole bh) {
        // List
        state.controller.getParams().put("max", "10");
        Map listResult = state.controller.listPeople();
        bh.consume(listResult);
        state.controller.reset();

        // Show first person
        List<Person> people = state.dynamicService.getPeople();
        if (people.isEmpty()) return null;
        Person person = people.get(0);
        state.controller.getParams().put("id", person.getId().toString());
        Map showResult = state.controller.showPerson();
        bh.consume(showResult);
        state.controller.reset();

        // Update
        state.controller.getParams().put("id", person.getId().toString());
        state.controller.getParams().put("firstName", "Updated");
        state.controller.getParams().put("lastName", person.getLastName());
        state.controller.getParams().put("email", person.getEmail());
        Map updateResult = state.controller.updatePerson();
        bh.consume(updateResult);

        return updateResult;
    }

    /**
     * Order processing cycle: create -> process -> show.
     */
    @Benchmark
    public Object request_orderCycle(ApplicationState state, Blackhole bh) {
        List<Person> people = state.dynamicService.getPeople();
        Person customer = people.get(state.random.nextInt(people.size()));

        // Create order
        state.controller.getParams().put("customerId", customer.getId().toString());
        state.controller.getParams().put("itemCount", "2");
        state.controller.getParams().put("items[0].productCode", "TEST-1");
        state.controller.getParams().put("items[0].productName", "Test Product 1");
        state.controller.getParams().put("items[0].quantity", "1");
        state.controller.getParams().put("items[0].unitPrice", "49.99");
        state.controller.getParams().put("items[1].productCode", "TEST-2");
        state.controller.getParams().put("items[1].productName", "Test Product 2");
        state.controller.getParams().put("items[1].quantity", "2");
        state.controller.getParams().put("items[1].unitPrice", "29.99");

        Map createResult = state.controller.createOrder();
        bh.consume(createResult);

        // Get order ID from redirect URL
        List<Order> orders = state.dynamicService.getOrders();
        if (orders.isEmpty()) return createResult;
        Order newOrder = orders.get(orders.size() - 1);
        state.controller.reset();

        // Process order
        state.controller.getParams().put("id", newOrder.getId().toString());
        Map processResult = state.controller.processOrder();
        bh.consume(processResult);
        state.controller.reset();

        // Show order
        state.controller.getParams().put("id", newOrder.getId().toString());
        Map showResult = state.controller.showOrder();
        bh.consume(showResult);

        return showResult;
    }
}
