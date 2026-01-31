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
package org.apache.groovy.bench.grailslike.service

import org.apache.groovy.bench.grailslike.model.*

/**
 * Simulates a Grails service with dynamic method dispatch patterns.
 * Services in Grails often use:
 * - Dynamic finders (methodMissing)
 * - Property access on domain objects
 * - Collection operations (each, collect, findAll)
 * - Builder patterns
 */
class DynamicService {

    // Simulated "database" - lists of domain objects
    List<Person> people = []
    List<Order> orders = []
    List<Product> products = []
    List<Customer> customers = []

    /**
     * methodMissing to simulate GORM dynamic finders.
     * e.g., findPersonByEmail, findAllOrdersByStatus
     */
    def methodMissing(String name, args) {
        if (name.startsWith('findAllBy')) {
            return handleFindAllBy(name, args)
        } else if (name.startsWith('findBy')) {
            return handleFindBy(name, args)
        } else if (name.startsWith('countBy')) {
            return handleCountBy(name, args)
        }
        throw new MissingMethodException(name, this.class, args)
    }

    private List handleFindAllBy(String name, args) {
        def (entityName, propertyName) = parseDynamicFinderName(name.substring(9))
        def collection = getCollectionForEntity(entityName)
        def value = args[0]
        collection.findAll { it."$propertyName" == value }
    }

    private Object handleFindBy(String name, args) {
        def (entityName, propertyName) = parseDynamicFinderName(name.substring(6))
        def collection = getCollectionForEntity(entityName)
        def value = args[0]
        collection.find { it."$propertyName" == value }
    }

    private int handleCountBy(String name, args) {
        def (entityName, propertyName) = parseDynamicFinderName(name.substring(7))
        def collection = getCollectionForEntity(entityName)
        def value = args[0]
        collection.count { it."$propertyName" == value }
    }

    private List parseDynamicFinderName(String suffix) {
        // Simple parser: PersonEmail -> [Person, email]
        def match = suffix =~ /^([A-Z][a-z]+)([A-Z].*)$/
        if (match) {
            def entity = match[0][1]
            def property = match[0][2].uncapitalize()
            return [entity, property]
        }
        [suffix, 'id']
    }

    private List getCollectionForEntity(String entityName) {
        switch (entityName) {
            case 'Person': return people
            case 'Order': return orders
            case 'Product': return products
            case 'Customer': return customers
            default: return []
        }
    }

    // ========================================================================
    // Person Service Methods
    // ========================================================================

    Person createPerson(Map params) {
        def person = new Person(
            firstName: params.firstName,
            lastName: params.lastName,
            email: params.email,
            birthDate: params.birthDate,
            phone: params.phone
        )
        if (params.address) {
            person.address = new Address(params.address)
        }
        if (person.save()) {
            people.add(person)
            return person
        }
        null
    }

    Person updatePerson(Long id, Map params) {
        def person = people.find { it.id == id }
        if (!person) return null

        params.each { key, value ->
            if (person.hasProperty(key)) {
                person."$key" = value
            }
        }
        person.save()
        person
    }

    List<Person> findPeopleWithOrders() {
        people.findAll { !it.orders.isEmpty() }
    }

    List<Map> getPeopleSummary() {
        people.collect { p ->
            [
                id: p.id,
                fullName: p.fullName,
                email: p.email,
                orderCount: p.orders.size(),
                totalSpent: p.orders.sum { it.total } ?: 0
            ]
        }
    }

    // ========================================================================
    // Order Service Methods
    // ========================================================================

    Order createOrder(Person customer, List<Map> itemsData) {
        def order = new Order(
            orderNumber: Order.generateOrderNumber(),
            orderDate: new Date(),
            customer: customer
        )

        itemsData.each { itemData ->
            def item = new OrderItem(
                productCode: itemData.productCode,
                productName: itemData.productName,
                quantity: itemData.quantity ?: 1,
                unitPrice: itemData.unitPrice ?: BigDecimal.ZERO
            )
            order.addToItems(item)
        }

        if (order.save()) {
            orders.add(order)
            customer.addToOrders(order)
            return order
        }
        null
    }

    Order processOrder(Long orderId) {
        def order = orders.find { it.id == orderId }
        if (!order) return null

        // Simulate order processing with heavy property access
        def summary = [
            orderNumber: order.orderNumber,
            customer: order.customer.fullName,
            items: order.items.collect { item ->
                [
                    product: item.productName,
                    qty: item.quantity,
                    price: item.unitPrice,
                    total: item.lineTotal
                ]
            },
            subtotal: order.subtotal,
            tax: order.tax,
            total: order.total
        ]

        // Update status
        order.status = OrderStatus.CONFIRMED
        order.save()

        order
    }

    List<Order> getOrdersByStatus(OrderStatus status) {
        orders.findAll { it.status == status }
    }

    List<Map> getOrdersReport() {
        orders.collect { o ->
            [
                orderNumber: o.orderNumber,
                customerName: o.customer?.fullName,
                itemCount: o.itemCount,
                total: o.total,
                status: o.status?.name()
            ]
        }
    }

    Map getOrderStatistics() {
        [
            totalOrders: orders.size(),
            pendingOrders: orders.count { it.status == OrderStatus.PENDING },
            completedOrders: orders.count { it.status == OrderStatus.COMPLETED },
            totalRevenue: orders.findAll { it.status == OrderStatus.COMPLETED }.sum { it.total } ?: 0,
            averageOrderValue: orders ? (orders.sum { it.total } / orders.size()) : 0
        ]
    }

    // ========================================================================
    // Batch Operations (common in Grails services)
    // ========================================================================

    int updateOrderStatuses(OrderStatus fromStatus, OrderStatus toStatus) {
        def toUpdate = orders.findAll { it.status == fromStatus }
        toUpdate.each { order ->
            order.status = toStatus
            order.save()
        }
        toUpdate.size()
    }

    List<Map> exportOrders(Date fromDate, Date toDate) {
        orders.findAll { o ->
            o.orderDate >= fromDate && o.orderDate <= toDate
        }.collect { o ->
            o.toMap()
        }
    }

    // ========================================================================
    // Complex Query Simulations
    // ========================================================================

    List<Map> getTopCustomers(int limit) {
        def customerTotals = orders.groupBy { it.customer }
            .collectEntries { customer, customerOrders ->
                [customer, customerOrders.sum { it.total }]
            }

        customerTotals.sort { -it.value }
            .take(limit)
            .collect { customer, total ->
                [
                    customerId: customer.id,
                    customerName: customer.fullName,
                    totalSpent: total,
                    orderCount: orders.count { it.customer == customer }
                ]
            }
    }

    List<Map> getProductSales() {
        def allItems = orders.collectMany { it.items }

        allItems.groupBy { it.productCode }
            .collectEntries { code, items ->
                [code, [
                    productCode: code,
                    productName: items[0].productName,
                    quantitySold: items.sum { it.quantity },
                    revenue: items.sum { it.lineTotal }
                ]]
            }
            .values()
            .sort { -it.revenue }
    }
}
