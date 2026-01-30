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
package org.apache.groovy.bench.grailslike.model

/**
 * Order domain object simulating a typical e-commerce order.
 * Has belongsTo relationship with Person and hasMany with OrderItem.
 */
class Order extends DomainObject {
    String orderNumber
    Date orderDate
    OrderStatus status = OrderStatus.PENDING
    String shippingMethod
    String notes

    // belongsTo relationship
    Person customer

    // hasMany relationship
    List<OrderItem> items = []

    // Shipping address (can differ from customer address)
    Address shippingAddress

    // Computed properties (heavy property access pattern)
    BigDecimal getSubtotal() {
        items.sum { it.lineTotal } ?: BigDecimal.ZERO
    }

    BigDecimal getTaxRate() {
        new BigDecimal("0.08") // 8% tax
    }

    BigDecimal getTax() {
        subtotal * taxRate
    }

    BigDecimal getTotal() {
        subtotal + tax
    }

    int getItemCount() {
        items.sum { it.quantity } ?: 0
    }

    // Simulates GORM's addToItems()
    Order addToItems(OrderItem item) {
        item.order = this
        items.add(item)
        this
    }

    // Simulates GORM's removeFromItems()
    Order removeFromItems(OrderItem item) {
        items.remove(item)
        item.order = null
        this
    }

    /**
     * Business logic method - common pattern in Grails services/domain.
     */
    boolean canCancel() {
        status in [OrderStatus.PENDING, OrderStatus.CONFIRMED]
    }

    boolean cancel() {
        if (!canCancel()) {
            errors.add("Order cannot be cancelled in status: $status")
            return false
        }
        status = OrderStatus.CANCELLED
        true
    }

    boolean ship(String trackingNumber) {
        if (status != OrderStatus.CONFIRMED) {
            errors.add("Order must be confirmed before shipping")
            return false
        }
        status = OrderStatus.SHIPPED
        dynamicProperties['trackingNumber'] = trackingNumber
        true
    }

    boolean complete() {
        if (status != OrderStatus.SHIPPED) {
            errors.add("Order must be shipped before completing")
            return false
        }
        status = OrderStatus.COMPLETED
        true
    }

    @Override
    protected void doValidate() {
        if (!orderNumber?.trim()) {
            errors.add("orderNumber cannot be blank")
        }
        if (!customer) {
            errors.add("customer is required")
        }
        if (items.isEmpty()) {
            errors.add("order must have at least one item")
        }
        // Validate each item
        items.each { item ->
            if (!item.validate()) {
                errors.addAll(item.errors.collect { "Item: $it" })
            }
        }
    }

    @Override
    Map toMap() {
        def result = super.toMap()
        result.putAll([
            orderNumber: orderNumber,
            orderDate: orderDate,
            status: status?.name(),
            customerId: customer?.id,
            customerName: customer?.fullName,
            shippingMethod: shippingMethod,
            shippingAddress: shippingAddress?.toMap(),
            items: items*.toMap(),
            itemCount: itemCount,
            subtotal: subtotal,
            tax: tax,
            total: total,
            notes: notes
        ])
        result
    }

    /**
     * Simulates dynamic finder: findByOrderNumber
     */
    static Order findByOrderNumber(List<Order> orders, String orderNumber) {
        orders.find { it.orderNumber == orderNumber }
    }

    /**
     * Simulates dynamic finder: findAllByStatus
     */
    static List<Order> findAllByStatus(List<Order> orders, OrderStatus status) {
        orders.findAll { it.status == status }
    }

    /**
     * Simulates criteria query: recent orders
     */
    static List<Order> findRecentOrders(List<Order> orders, int days) {
        def cutoff = new Date() - days
        orders.findAll { it.orderDate >= cutoff }
              .sort { -it.orderDate.time }
    }

    /**
     * Generate unique order number.
     */
    static String generateOrderNumber() {
        "ORD-${System.currentTimeMillis()}-${(Math.random() * 10000).intValue()}"
    }
}

/**
 * Order status enum.
 */
enum OrderStatus {
    PENDING,
    CONFIRMED,
    SHIPPED,
    COMPLETED,
    CANCELLED
}
