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
 * OrderItem domain object representing a line item in an order.
 * Demonstrates nested object access patterns common in Grails views.
 */
class OrderItem extends DomainObject {
    String productCode
    String productName
    String description
    int quantity = 1
    BigDecimal unitPrice = BigDecimal.ZERO
    BigDecimal discount = BigDecimal.ZERO

    // belongsTo relationship
    Order order

    // Product category for filtering
    String category

    // Computed property: line total
    BigDecimal getLineTotal() {
        def base = unitPrice * quantity
        base - (base * discount)
    }

    // Computed property: savings
    BigDecimal getSavings() {
        unitPrice * quantity * discount
    }

    // Computed property: has discount
    boolean getHasDiscount() {
        discount > BigDecimal.ZERO
    }

    /**
     * Update quantity with validation.
     */
    boolean updateQuantity(int newQuantity) {
        if (newQuantity < 1) {
            errors.add("quantity must be at least 1")
            return false
        }
        if (newQuantity > 100) {
            errors.add("quantity cannot exceed 100")
            return false
        }
        this.quantity = newQuantity
        true
    }

    /**
     * Apply discount with validation.
     */
    boolean applyDiscount(BigDecimal discountRate) {
        if (discountRate < 0 || discountRate > 1) {
            errors.add("discount must be between 0 and 1")
            return false
        }
        this.discount = discountRate
        true
    }

    @Override
    protected void doValidate() {
        if (!productCode?.trim()) {
            errors.add("productCode cannot be blank")
        }
        if (!productName?.trim()) {
            errors.add("productName cannot be blank")
        }
        if (quantity < 1) {
            errors.add("quantity must be at least 1")
        }
        if (unitPrice < BigDecimal.ZERO) {
            errors.add("unitPrice cannot be negative")
        }
        if (discount < BigDecimal.ZERO || discount > BigDecimal.ONE) {
            errors.add("discount must be between 0 and 1")
        }
    }

    @Override
    Map toMap() {
        def result = super.toMap()
        result.putAll([
            productCode: productCode,
            productName: productName,
            description: description,
            category: category,
            quantity: quantity,
            unitPrice: unitPrice,
            discount: discount,
            lineTotal: lineTotal,
            savings: savings,
            hasDiscount: hasDiscount,
            orderId: order?.id
        ])
        result
    }

    /**
     * Factory method for creating test items.
     */
    static OrderItem create(String code, String name, int qty, BigDecimal price) {
        new OrderItem(
            productCode: code,
            productName: name,
            quantity: qty,
            unitPrice: price
        )
    }

    /**
     * Factory method with discount.
     */
    static OrderItem createWithDiscount(String code, String name, int qty, BigDecimal price, BigDecimal discount) {
        def item = create(code, name, qty, price)
        item.discount = discount
        item
    }
}

/**
 * Product catalog for generating test data.
 */
class ProductCatalog {
    static final List<Map> PRODUCTS = [
        [code: 'LAPTOP-001', name: 'Business Laptop', category: 'Electronics', price: 999.99],
        [code: 'LAPTOP-002', name: 'Gaming Laptop', category: 'Electronics', price: 1499.99],
        [code: 'PHONE-001', name: 'Smartphone Pro', category: 'Electronics', price: 799.99],
        [code: 'PHONE-002', name: 'Smartphone Basic', category: 'Electronics', price: 399.99],
        [code: 'TABLET-001', name: 'Tablet 10"', category: 'Electronics', price: 599.99],
        [code: 'CHAIR-001', name: 'Office Chair', category: 'Furniture', price: 299.99],
        [code: 'DESK-001', name: 'Standing Desk', category: 'Furniture', price: 499.99],
        [code: 'MONITOR-001', name: '27" Monitor', category: 'Electronics', price: 349.99],
        [code: 'KEYBOARD-001', name: 'Mechanical Keyboard', category: 'Accessories', price: 149.99],
        [code: 'MOUSE-001', name: 'Wireless Mouse', category: 'Accessories', price: 79.99],
        [code: 'HEADSET-001', name: 'Wireless Headset', category: 'Accessories', price: 199.99],
        [code: 'WEBCAM-001', name: 'HD Webcam', category: 'Accessories', price: 129.99],
        [code: 'BOOK-001', name: 'Programming Guide', category: 'Books', price: 49.99],
        [code: 'BOOK-002', name: 'Architecture Patterns', category: 'Books', price: 59.99],
        [code: 'SOFTWARE-001', name: 'IDE License', category: 'Software', price: 199.99],
    ]

    static OrderItem randomItem(Random random = new Random()) {
        def product = PRODUCTS[random.nextInt(PRODUCTS.size())]
        OrderItem.create(
            product.code as String,
            product.name as String,
            random.nextInt(5) + 1,
            new BigDecimal(product.price.toString())
        )
    }

    static List<OrderItem> randomItems(int count, Random random = new Random()) {
        (1..count).collect { randomItem(random) }
    }
}
