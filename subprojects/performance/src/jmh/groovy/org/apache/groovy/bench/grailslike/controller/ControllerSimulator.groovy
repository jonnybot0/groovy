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
package org.apache.groovy.bench.grailslike.controller

import org.apache.groovy.bench.grailslike.model.*
import org.apache.groovy.bench.grailslike.service.*

/**
 * Simulates a Grails controller with typical request handling patterns.
 * Controllers in Grails exercise:
 * - Parameter binding (params map access)
 * - Service method calls (dynamic dispatch)
 * - Model building for views
 * - Redirect/render decisions
 */
class ControllerSimulator {

    // Injected services
    DynamicService dynamicService
    TransactionalSimulator transactionalService

    // Request context simulation
    Map params = [:]
    Map model = [:]
    Map flash = [:]
    String redirectUrl
    String renderView

    // Session simulation
    Map session = [:]

    /**
     * Reset controller state for new request.
     */
    void reset() {
        params.clear()
        model.clear()
        redirectUrl = null
        renderView = null
    }

    // ========================================================================
    // Person Controller Actions
    // ========================================================================

    /**
     * List action - common index page pattern.
     */
    Map listPeople() {
        def max = ParamsHelper.parseInt(params,'max', 10)
        def offset = ParamsHelper.parseInt(params,'offset', 0)
        def sort = params.sort ?: 'lastName'
        def order = params.order ?: 'asc'

        def people = dynamicService.people
        def total = people.size()

        // Sort
        def sorted = people.sort { a, b ->
            def result = a."$sort" <=> b."$sort"
            order == 'desc' ? -result : result
        }

        // Paginate
        def list = sorted.drop(offset).take(max)

        model = [
            personList: list,
            personCount: total,
            params: [max: max, offset: offset, sort: sort, order: order]
        ]
        renderView = 'list'
        model
    }

    /**
     * Show action - detail page pattern.
     */
    Map showPerson() {
        def id = ParamsHelper.parseLong(params,'id')
        def person = dynamicService.people.find { it.id == id }

        if (!person) {
            flash.error = "Person not found"
            redirectUrl = '/person/list'
            return null
        }

        model = [
            person: person,
            orders: person.orders,
            totalSpent: person.orders.sum { it.total } ?: 0
        ]
        renderView = 'show'
        model
    }

    /**
     * Create action - form submission pattern.
     */
    Map createPerson() {
        def person = dynamicService.createPerson([
            firstName: params.firstName,
            lastName: params.lastName,
            email: params.email,
            phone: params.phone,
            address: params.address ? [
                street: params.'address.street',
                city: params.'address.city',
                state: params.'address.state',
                zipCode: params.'address.zipCode'
            ] : null
        ])

        if (person) {
            flash.message = "Person created successfully"
            redirectUrl = "/person/show/${person.id}"
            return [person: person]
        } else {
            flash.error = "Failed to create person"
            model = [person: new Person(params)]
            renderView = 'create'
            return model
        }
    }

    /**
     * Update action - edit form submission.
     */
    Map updatePerson() {
        def id = ParamsHelper.parseLong(params,'id')
        def person = dynamicService.updatePerson(id, params.subMap([
            'firstName', 'lastName', 'email', 'phone'
        ]))

        if (person) {
            flash.message = "Person updated successfully"
            redirectUrl = "/person/show/$id"
            return [person: person]
        } else {
            flash.error = "Failed to update person"
            redirectUrl = "/person/edit/$id"
            return null
        }
    }

    // ========================================================================
    // Order Controller Actions
    // ========================================================================

    /**
     * List orders with filtering.
     */
    Map listOrders() {
        def status = params.status ? OrderStatus.valueOf(params.status) : null
        def customerId = ParamsHelper.parseLong(params,'customerId')

        def orders = dynamicService.orders
        if (status) {
            orders = orders.findAll { it.status == status }
        }
        if (customerId) {
            orders = orders.findAll { it.customer?.id == customerId }
        }

        model = [
            orderList: orders,
            orderCount: orders.size(),
            statusOptions: OrderStatus.values()
        ]
        renderView = 'list'
        model
    }

    /**
     * Show order with all related data.
     */
    Map showOrder() {
        def id = ParamsHelper.parseLong(params,'id')
        def order = dynamicService.orders.find { it.id == id }

        if (!order) {
            flash.error = "Order not found"
            redirectUrl = '/order/list'
            return null
        }

        // Heavy property access for view model
        model = [
            order: order,
            customer: order.customer,
            items: order.items,
            itemCount: order.itemCount,
            subtotal: order.subtotal,
            tax: order.tax,
            total: order.total,
            canCancel: order.canCancel(),
            statusHistory: [] // Would be loaded from audit log
        ]
        renderView = 'show'
        model
    }

    /**
     * Create order from shopping cart.
     */
    Map createOrder() {
        def customerId = ParamsHelper.parseLong(params,'customerId')
        def customer = dynamicService.people.find { it.id == customerId }

        if (!customer) {
            flash.error = "Customer not found"
            redirectUrl = '/order/list'
            return null
        }

        // Parse items from params (common pattern)
        def items = []
        def itemCount = ParamsHelper.parseInt(params,'itemCount', 0)
        (0..<itemCount).each { i ->
            items << [
                productCode: params."items[$i].productCode",
                productName: params."items[$i].productName",
                quantity: ParamsHelper.parseInt(params,"items[$i].quantity", 1),
                unitPrice: new BigDecimal(params."items[$i].unitPrice" ?: "0")
            ]
        }

        def order = dynamicService.createOrder(customer, items)

        if (order) {
            flash.message = "Order ${order.orderNumber} created"
            redirectUrl = "/order/show/${order.id}"
            return [order: order]
        } else {
            flash.error = "Failed to create order"
            model = [customer: customer, items: items]
            renderView = 'create'
            return model
        }
    }

    /**
     * Process order action.
     */
    Map processOrder() {
        def id = ParamsHelper.parseLong(params,'id')
        def order = dynamicService.processOrder(id)

        if (order) {
            flash.message = "Order ${order.orderNumber} processed"
        } else {
            flash.error = "Failed to process order"
        }
        redirectUrl = "/order/show/$id"
        [order: order]
    }

    // ========================================================================
    // Dashboard/Report Actions
    // ========================================================================

    /**
     * Dashboard with aggregated data - heavy computation.
     */
    Map dashboard() {
        def stats = dynamicService.getOrderStatistics()
        def topCustomers = dynamicService.getTopCustomers(5)
        def recentOrders = dynamicService.orders
            .sort { -it.orderDate.time }
            .take(10)

        model = [
            stats: stats,
            topCustomers: topCustomers,
            recentOrders: recentOrders,
            ordersByStatus: OrderStatus.values().collectEntries { status ->
                [status, dynamicService.orders.count { it.status == status }]
            }
        ]
        renderView = 'dashboard'
        model
    }

    /**
     * Export action - generates large data structure.
     */
    Map exportOrders() {
        def fromDate = ParamsHelper.parseDate(params,'fromDate', 'yyyy-MM-dd') ?: (new Date() - 30)
        def toDate = ParamsHelper.parseDate(params,'toDate', 'yyyy-MM-dd') ?: new Date()

        def data = dynamicService.exportOrders(fromDate, toDate)

        model = [
            data: data,
            fromDate: fromDate,
            toDate: toDate,
            recordCount: data.size()
        ]
        renderView = 'export'
        model
    }

    // ========================================================================
    // AJAX/JSON Actions
    // ========================================================================

    /**
     * JSON response action.
     */
    Map jsonOrderDetails() {
        def id = ParamsHelper.parseLong(params,'id')
        def order = dynamicService.orders.find { it.id == id }

        if (!order) {
            return [error: 'Order not found', status: 404]
        }

        // Build JSON response with nested objects
        [
            order: [
                id: order.id,
                orderNumber: order.orderNumber,
                status: order.status?.name(),
                customer: [
                    id: order.customer?.id,
                    name: order.customer?.fullName,
                    email: order.customer?.email
                ],
                items: order.items.collect { item ->
                    [
                        productCode: item.productCode,
                        productName: item.productName,
                        quantity: item.quantity,
                        unitPrice: item.unitPrice,
                        lineTotal: item.lineTotal
                    ]
                },
                totals: [
                    subtotal: order.subtotal,
                    tax: order.tax,
                    total: order.total
                ]
            ]
        ]
    }

    /**
     * Autocomplete/search action.
     */
    List searchPeople() {
        def term = params.term?.toLowerCase() ?: ''
        def max = ParamsHelper.parseInt(params,'max', 10)

        dynamicService.people
            .findAll { p ->
                p.fullName.toLowerCase().contains(term) ||
                p.email?.toLowerCase()?.contains(term)
            }
            .take(max)
            .collect { p ->
                [id: p.id, label: p.fullName, value: p.email]
            }
    }
}

/**
 * Utility class for parsing parameters from a Map.
 * Similar to Grails' params object.
 */
class ParamsHelper {
    static Long parseLong(Map params, String key, Long defaultValue = null) {
        def value = params[key]
        if (value == null) return defaultValue
        try {
            return value.toString().toLong()
        } catch (NumberFormatException e) {
            return defaultValue
        }
    }

    static Integer parseInt(Map params, String key, Integer defaultValue = null) {
        def value = params[key]
        if (value == null) return defaultValue
        try {
            return value.toString().toInteger()
        } catch (NumberFormatException e) {
            return defaultValue
        }
    }

    static Date parseDate(Map params, String key, String format, Date defaultValue = null) {
        def value = params[key]
        if (value == null) return defaultValue
        try {
            return Date.parse(format, value.toString())
        } catch (Exception e) {
            return defaultValue
        }
    }
}
