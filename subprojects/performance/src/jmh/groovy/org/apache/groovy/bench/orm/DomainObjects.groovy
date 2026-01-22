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
 * Domain object classes that simulate GORM/Grails domain patterns.
 * These are used to benchmark property access, relationship traversal,
 * and dynamic finder-like patterns.
 */

/**
 * Base trait for domain objects with common patterns.
 */
trait DomainEntity {
    Long id
    Long version
    Date dateCreated
    Date lastUpdated

    // Simulate dirty checking
    private Set<String> dirtyProperties = []

    void markDirty(String propertyName) {
        dirtyProperties.add(propertyName)
    }

    boolean isDirty() {
        !dirtyProperties.isEmpty()
    }

    Set<String> getDirtyPropertyNames() {
        dirtyProperties
    }

    void clearDirty() {
        dirtyProperties.clear()
    }
}

/**
 * Person domain class with relationships.
 */
class PersonDomain implements DomainEntity {
    String firstName
    String lastName
    String email
    Integer age
    Boolean active = true

    AddressDomain address
    List<OrderDomain> orders = []

    String getFullName() {
        "$firstName $lastName"
    }

    String getDisplayName() {
        active ? fullName : "[$fullName] (inactive)"
    }

    // Simulate GORM dynamic finder
    static PersonDomain findByEmail(String email, List<PersonDomain> all) {
        all.find { it.email == email }
    }

    static List<PersonDomain> findAllByActive(Boolean active, List<PersonDomain> all) {
        all.findAll { it.active == active }
    }

    static List<PersonDomain> findAllByLastNameLike(String pattern, List<PersonDomain> all) {
        all.findAll { it.lastName?.contains(pattern) }
    }
}

/**
 * Address domain class (embedded-like).
 */
class AddressDomain implements DomainEntity {
    String street
    String city
    String state
    String zipCode
    String country

    String getFullAddress() {
        "$street, $city, $state $zipCode, $country"
    }

    String getShortAddress() {
        "$city, $state"
    }
}

/**
 * Order domain class with line items.
 */
class OrderDomain implements DomainEntity {
    String orderNumber
    Date orderDate
    String status
    PersonDomain customer

    List<OrderItemDomain> items = []

    BigDecimal getTotal() {
        items.sum { it.lineTotal } ?: 0.0
    }

    Integer getItemCount() {
        items.size()
    }

    // Simulate adding item
    void addItem(ProductDomain product, Integer quantity) {
        def item = new OrderItemDomain(
            order: this,
            product: product,
            quantity: quantity,
            unitPrice: product.price
        )
        items.add(item)
    }
}

/**
 * Order item domain class.
 */
class OrderItemDomain implements DomainEntity {
    OrderDomain order
    ProductDomain product
    Integer quantity
    BigDecimal unitPrice

    BigDecimal getLineTotal() {
        (unitPrice ?: 0.0) * (quantity ?: 0)
    }

    String getDescription() {
        "${product?.name} x $quantity"
    }
}

/**
 * Product domain class.
 */
class ProductDomain implements DomainEntity {
    String sku
    String name
    String description
    BigDecimal price
    Integer stockQuantity
    CategoryDomain category

    Boolean isInStock() {
        stockQuantity > 0
    }

    String getDisplayPrice() {
        "\$${price?.setScale(2)}"
    }
}

/**
 * Category domain class with hierarchy.
 */
class CategoryDomain implements DomainEntity {
    String name
    String code
    CategoryDomain parent
    List<CategoryDomain> children = []

    String getFullPath() {
        parent ? "${parent.fullPath} > $name" : name
    }

    List<CategoryDomain> getAncestors() {
        def result = []
        def current = parent
        while (current) {
            result.add(0, current)
            current = current.parent
        }
        result
    }
}

/**
 * Factory for creating test domain objects.
 */
class DomainFactory {

    static PersonDomain createPerson(int index) {
        def person = new PersonDomain(
            id: index,
            firstName: "First$index",
            lastName: "Last$index",
            email: "user$index@example.com",
            age: 20 + (index % 50),
            active: index % 10 != 0,
            dateCreated: new Date(),
            lastUpdated: new Date()
        )

        person.address = createAddress(index)
        return person
    }

    static AddressDomain createAddress(int index) {
        new AddressDomain(
            id: index,
            street: "$index Main Street",
            city: "City${index % 100}",
            state: ['CA', 'NY', 'TX', 'FL', 'WA'][index % 5],
            zipCode: String.format("%05d", index % 100000),
            country: "USA",
            dateCreated: new Date(),
            lastUpdated: new Date()
        )
    }

    static ProductDomain createProduct(int index) {
        new ProductDomain(
            id: index,
            sku: "SKU-$index",
            name: "Product $index",
            description: "Description for product $index",
            price: 10.0 + (index % 100),
            stockQuantity: index % 2 == 0 ? index : 0,
            dateCreated: new Date(),
            lastUpdated: new Date()
        )
    }

    static OrderDomain createOrder(PersonDomain customer, List<ProductDomain> products, int index) {
        def order = new OrderDomain(
            id: index,
            orderNumber: "ORD-${String.format("%06d", index)}",
            orderDate: new Date(),
            status: ['PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED'][index % 4],
            customer: customer,
            dateCreated: new Date(),
            lastUpdated: new Date()
        )

        // Add 1-5 items per order
        int itemCount = 1 + (index % 5)
        for (int i = 0; i < itemCount && i < products.size(); i++) {
            order.addItem(products[(index + i) % products.size()], 1 + (i % 3))
        }

        return order
    }

    static CategoryDomain createCategoryHierarchy(int depth, int breadth) {
        def root = new CategoryDomain(id: 0, name: "Root", code: "ROOT")
        createCategoryChildren(root, depth, breadth, 1)
        return root
    }

    private static int createCategoryChildren(CategoryDomain parent, int depth, int breadth, int startId) {
        if (depth <= 0) return startId

        int currentId = startId
        for (int i = 0; i < breadth; i++) {
            def child = new CategoryDomain(
                id: currentId,
                name: "${parent.name}-Child$i",
                code: "${parent.code}-C$i",
                parent: parent
            )
            parent.children.add(child)
            currentId++
            currentId = createCategoryChildren(child, depth - 1, breadth, currentId)
        }
        return currentId
    }
}

/**
 * Operations that simulate common GORM/Grails patterns.
 */
class DomainOperations {

    /**
     * Simulate view rendering: access multiple properties.
     */
    static Map renderPersonView(PersonDomain person) {
        [
            fullName: person.fullName,
            email: person.email,
            age: person.age,
            active: person.active,
            address: person.address?.shortAddress,
            orderCount: person.orders?.size() ?: 0
        ]
    }

    /**
     * Simulate list view: access properties on collection.
     */
    static List<Map> renderPersonList(List<PersonDomain> people) {
        people.collect { person ->
            [
                id: person.id,
                name: person.fullName,
                email: person.email,
                active: person.active
            ]
        }
    }

    /**
     * Simulate order summary calculation.
     */
    static Map calculateOrderSummary(OrderDomain order) {
        [
            orderNumber: order.orderNumber,
            customerName: order.customer?.fullName,
            itemCount: order.itemCount,
            total: order.total,
            status: order.status
        ]
    }

    /**
     * Simulate report generation with aggregation.
     */
    static Map generateCustomerReport(PersonDomain person) {
        def orders = person.orders
        [
            customer: person.fullName,
            totalOrders: orders.size(),
            totalSpent: orders.sum { it.total } ?: 0.0,
            averageOrderValue: orders ? (orders.sum { it.total } / orders.size()) : 0.0,
            lastOrderDate: orders.max { it.orderDate }?.orderDate
        ]
    }

    /**
     * Traverse category hierarchy.
     */
    static List<String> getCategoryPath(CategoryDomain category) {
        category.ancestors*.name + [category.name]
    }

    /**
     * Deep graph traversal.
     */
    static List<Map> getOrderDetails(OrderDomain order) {
        order.items.collect { item ->
            [
                product: item.product.name,
                sku: item.product.sku,
                category: item.product.category?.fullPath,
                quantity: item.quantity,
                unitPrice: item.unitPrice,
                lineTotal: item.lineTotal
            ]
        }
    }
}
