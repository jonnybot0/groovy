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
package org.apache.groovy.bench.grailslike.view;

import org.apache.groovy.bench.grailslike.model.*;
import org.apache.groovy.bench.grailslike.service.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Benchmarks simulating GSP-like template rendering.
 * <p>
 * GSP templates heavily exercise:
 * <ul>
 *   <li>Property access on model objects (${person.name})</li>
 *   <li>Collection iteration (g:each)</li>
 *   <li>Conditional rendering (g:if)</li>
 *   <li>GString interpolation</li>
 *   <li>Tag invocations (dynamic method calls)</li>
 * </ul>
 * <p>
 * Run with: ./gradlew -Pindy=true -PbenchInclude=TemplateRender :perf:jmh
 */
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(2)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class TemplateRenderBench {

    // ========================================================================
    // State classes
    // ========================================================================

    @State(Scope.Thread)
    public static class ViewState {
        TemplateSimulator templateSimulator;
        DynamicService dynamicService;
        Random random;

        // Pre-built models for benchmarks
        Map<String, Object> personModel;
        Map<String, Object> orderModel;
        Map<String, Object> listModel;
        Map<String, Object> dashboardModel;

        @Setup(Level.Trial)
        public void setup() {
            random = new Random(42);
            dynamicService = new DynamicService();
            templateSimulator = new TemplateSimulator();

            // Seed data
            seedTestData();

            // Pre-build models
            buildModels();
        }

        private void seedTestData() {
            // Create people with orders
            for (int i = 0; i < 100; i++) {
                Person person = new Person();
                person.setFirstName("First" + i);
                person.setLastName("Last" + i);
                person.setEmail("person" + i + "@example.com");

                Address addr = new Address();
                addr.setStreet(i + " Main St");
                addr.setCity("City" + (i % 10));
                addr.setState("ST");
                addr.setZipCode(String.format("%05d", i));
                person.setAddress(addr);

                person.save();
                dynamicService.getPeople().add(person);

                // Create orders
                if (i % 3 == 0) {
                    Order order = new Order();
                    order.setOrderNumber(Order.generateOrderNumber());
                    order.setOrderDate(new Date());
                    order.setCustomer(person);
                    order.setStatus(OrderStatus.values()[random.nextInt(OrderStatus.values().length)]);

                    for (int j = 0; j < random.nextInt(5) + 1; j++) {
                        order.addToItems(ProductCatalog.randomItem(random));
                    }

                    order.save();
                    dynamicService.getOrders().add(order);
                    person.addToOrders(order);
                }
            }
        }

        private void buildModels() {
            List<Person> people = dynamicService.getPeople();
            List<Order> orders = dynamicService.getOrders();

            // Person detail model
            Person person = people.get(0);
            personModel = new HashMap<>();
            personModel.put("person", person);
            personModel.put("orders", person.getOrders());

            // Order detail model
            if (!orders.isEmpty()) {
                Order order = orders.get(0);
                orderModel = new HashMap<>();
                orderModel.put("order", order);
                orderModel.put("customer", order.getCustomer());
                orderModel.put("items", order.getItems());
            }

            // List model
            listModel = new HashMap<>();
            listModel.put("personList", people);
            listModel.put("personCount", people.size());

            // Dashboard model
            dashboardModel = new HashMap<>();
            dashboardModel.put("recentOrders", orders.subList(0, Math.min(10, orders.size())));
            dashboardModel.put("stats", dynamicService.getOrderStatistics());
            dashboardModel.put("topCustomers", dynamicService.getTopCustomers(5));
        }
    }

    // ========================================================================
    // Simple Property Access Benchmarks
    // ========================================================================

    /**
     * Render person detail - simple property access.
     */
    @Benchmark
    public Object render_personDetail(ViewState state, Blackhole bh) {
        String html = state.templateSimulator.renderPersonDetail(state.personModel);
        bh.consume(html);
        return html;
    }

    /**
     * Render order detail - nested property access.
     */
    @Benchmark
    public Object render_orderDetail(ViewState state, Blackhole bh) {
        if (state.orderModel == null) return null;
        String html = state.templateSimulator.renderOrderDetail(state.orderModel);
        bh.consume(html);
        return html;
    }

    // ========================================================================
    // List/Table Rendering Benchmarks
    // ========================================================================

    /**
     * Render person list table - iteration + property access.
     */
    @Benchmark
    public Object render_personList(ViewState state, Blackhole bh) {
        String html = state.templateSimulator.renderPersonList(state.listModel);
        bh.consume(html);
        return html;
    }

    /**
     * Render order list with computed values.
     */
    @Benchmark
    public Object render_orderList(ViewState state, Blackhole bh) {
        Map<String, Object> model = new HashMap<>();
        model.put("orderList", state.dynamicService.getOrders());
        String html = state.templateSimulator.renderOrderList(model);
        bh.consume(html);
        return html;
    }

    // ========================================================================
    // Complex Template Benchmarks
    // ========================================================================

    /**
     * Render dashboard - multiple sections, aggregations.
     */
    @Benchmark
    public Object render_dashboard(ViewState state, Blackhole bh) {
        String html = state.templateSimulator.renderDashboard(state.dashboardModel);
        bh.consume(html);
        return html;
    }

    /**
     * Render order with line items - nested iteration.
     */
    @Benchmark
    public Object render_orderWithItems(ViewState state, Blackhole bh) {
        if (state.orderModel == null) return null;
        String html = state.templateSimulator.renderOrderWithItems(state.orderModel);
        bh.consume(html);
        return html;
    }

    // ========================================================================
    // Spread Operator Benchmarks
    // ========================================================================

    /**
     * Use spread operator for property extraction.
     */
    @Benchmark
    public Object render_spreadOperator(ViewState state, Blackhole bh) {
        String html = state.templateSimulator.renderWithSpreadOperator(state.listModel);
        bh.consume(html);
        return html;
    }

    // ========================================================================
    // Conditional Rendering Benchmarks
    // ========================================================================

    /**
     * Conditional rendering (g:if equivalent).
     */
    @Benchmark
    public Object render_conditional(ViewState state, Blackhole bh) {
        String html = state.templateSimulator.renderConditional(state.orderModel);
        bh.consume(html);
        return html;
    }

    // ========================================================================
    // GString Interpolation Benchmarks
    // ========================================================================

    /**
     * Heavy GString interpolation.
     */
    @Benchmark
    public Object render_gstringHeavy(ViewState state, Blackhole bh) {
        String html = state.templateSimulator.renderGStringHeavy(state.personModel);
        bh.consume(html);
        return html;
    }

    // ========================================================================
    // Full Page Render Benchmarks
    // ========================================================================

    /**
     * Full page render with layout, header, content, footer.
     */
    @Benchmark
    public Object render_fullPage(ViewState state, Blackhole bh) {
        String html = state.templateSimulator.renderFullPage(state.dashboardModel);
        bh.consume(html);
        return html;
    }
}
