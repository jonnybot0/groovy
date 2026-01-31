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

/**
 * Entity graph for traversal benchmarks.
 * Simulates GORM-style object relationships.
 */
class EntityGraph {

    List<GraphCustomer> customers = []

    void buildGraph(int customerCount) {
        def random = new Random(42)
        def categories = ['Electronics', 'Clothing', 'Books', 'Home', 'Sports'].collect {
            new GraphCategory(name: it, code: it.take(3).toUpperCase())
        }

        customerCount.times { i ->
            def customer = new GraphCustomer(
                name: "Customer $i",
                email: "customer${i}@example.com",
                address: new GraphAddress(
                    street: "${i * 10} Main St",
                    city: "City${i % 10}",
                    country: new GraphCountry(
                        name: "Country${i % 5}",
                        code: "C${i % 5}"
                    )
                )
            )

            // Add 1-5 orders per customer
            (random.nextInt(5) + 1).times { j ->
                def order = new GraphOrder(
                    orderNumber: "ORD-$i-$j",
                    customer: customer
                )

                // Add 1-10 items per order
                (random.nextInt(10) + 1).times { k ->
                    def product = new GraphProduct(
                        name: "Product $i-$j-$k",
                        sku: "SKU-$i-$j-$k",
                        category: categories[random.nextInt(categories.size())]
                    )
                    order.items << new GraphOrderItem(
                        product: product,
                        quantity: random.nextInt(5) + 1,
                        price: (random.nextInt(10000) + 100) / 100.0
                    )
                }

                customer.orders << order
            }

            customers << customer
        }
    }

    // ========================================================================
    // Traversal methods for benchmarks
    // ========================================================================

    void traverseSimpleProperty(Blackhole bh) {
        for (customer in customers) {
            bh.consume(customer.name)
            bh.consume(customer.email)
        }
    }

    void traverseNestedProperty(Blackhole bh) {
        for (customer in customers) {
            bh.consume(customer.address.city)
            bh.consume(customer.address.street)
        }
    }

    void traverseDeepProperty(Blackhole bh) {
        for (customer in customers) {
            bh.consume(customer.address.country.name)
            bh.consume(customer.address.country.code)
        }
    }

    void traverseVeryDeepProperty(Blackhole bh) {
        for (customer in customers) {
            for (order in customer.orders) {
                for (item in order.items) {
                    bh.consume(item.product.category.name)
                }
            }
        }
    }

    void traverseWithSpread(Blackhole bh) {
        for (customer in customers) {
            def orderNumbers = customer.orders*.orderNumber
            bh.consume(orderNumbers)
        }
    }

    void traverseNestedSpread(Blackhole bh) {
        for (customer in customers) {
            def productNames = customer.orders*.items.flatten()*.product*.name
            bh.consume(productNames)
        }
    }

    void traverseNullSafe(Blackhole bh) {
        for (customer in customers) {
            // Some might be null in real scenarios
            bh.consume(customer?.address?.country?.name)
            bh.consume(customer?.orders?.first()?.items?.first()?.product?.name)
        }
    }

    void traverseMixedPatterns(Blackhole bh) {
        for (customer in customers) {
            // Simple
            bh.consume(customer.name)

            // Nested
            bh.consume(customer.address.city)

            // Deep
            bh.consume(customer.address.country.name)

            // Spread
            bh.consume(customer.orders*.orderNumber)

            // Collection with closure
            def totals = customer.orders.collect { order ->
                order.items.sum { it.price * it.quantity }
            }
            bh.consume(totals)
        }
    }

    // ========================================================================
    // Java baseline methods
    // ========================================================================

    void javaTraverseSimple(Blackhole bh) {
        for (GraphCustomer customer : customers) {
            bh.consume(customer.getName())
            bh.consume(customer.getEmail())
        }
    }

    void javaTraverseDeep(Blackhole bh) {
        for (GraphCustomer customer : customers) {
            bh.consume(customer.getAddress().getCountry().getName())
            bh.consume(customer.getAddress().getCountry().getCode())
        }
    }
}

// Domain classes for graph traversal
class GraphCustomer {
    String name
    String email
    GraphAddress address
    List<GraphOrder> orders = []
}

class GraphAddress {
    String street
    String city
    GraphCountry country
}

class GraphCountry {
    String name
    String code
}

class GraphOrder {
    String orderNumber
    GraphCustomer customer
    List<GraphOrderItem> items = []
}

class GraphOrderItem {
    GraphProduct product
    int quantity
    BigDecimal price
}

class GraphProduct {
    String name
    String sku
    GraphCategory category
}

class GraphCategory {
    String name
    String code
}
