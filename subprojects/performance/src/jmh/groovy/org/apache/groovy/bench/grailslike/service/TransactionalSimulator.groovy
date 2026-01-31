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
 * Simulates transactional service patterns common in Grails.
 * Exercises:
 * - Nested method calls through dynamic dispatch
 * - Exception handling and rollback simulation
 * - Dirty checking patterns
 * - Flush and clear simulation
 */
class TransactionalSimulator {

    // Transaction state tracking
    private ThreadLocal<TransactionContext> currentTransaction = new ThreadLocal<>()

    // Services that would be injected in real Grails
    DynamicService dynamicService

    /**
     * Simulate @Transactional behavior with closure.
     */
    def <T> T withTransaction(Closure<T> closure) {
        def ctx = beginTransaction()
        try {
            def result = closure.call(ctx)
            commitTransaction(ctx)
            return result
        } catch (Exception e) {
            rollbackTransaction(ctx)
            throw e
        } finally {
            endTransaction()
        }
    }

    /**
     * Simulate @Transactional(readOnly = true)
     */
    def <T> T withReadOnlyTransaction(Closure<T> closure) {
        def ctx = beginTransaction()
        ctx.readOnly = true
        try {
            def result = closure.call(ctx)
            // Read-only transactions don't flush
            return result
        } finally {
            endTransaction()
        }
    }

    /**
     * Simulate nested transaction (@Transactional(propagation = REQUIRES_NEW))
     */
    def <T> T withNewTransaction(Closure<T> closure) {
        def parentCtx = currentTransaction.get()
        def ctx = beginTransaction()
        ctx.parent = parentCtx
        try {
            def result = closure.call(ctx)
            commitTransaction(ctx)
            return result
        } catch (Exception e) {
            rollbackTransaction(ctx)
            throw e
        } finally {
            currentTransaction.set(parentCtx)
        }
    }

    private TransactionContext beginTransaction() {
        def ctx = new TransactionContext()
        currentTransaction.set(ctx)
        ctx
    }

    private void commitTransaction(TransactionContext ctx) {
        if (ctx.readOnly) return

        // Simulate flush - check all dirty objects
        ctx.managedEntities.each { entity ->
            if (entity.isDirty()) {
                // Simulate write to database
                entity.save(flush: true)
            }
        }
        ctx.committed = true
    }

    private void rollbackTransaction(TransactionContext ctx) {
        // Simulate rollback - discard all changes
        ctx.managedEntities.each { entity ->
            entity.discard()
        }
        ctx.rolledBack = true
    }

    private void endTransaction() {
        currentTransaction.remove()
    }

    // ========================================================================
    // Transactional Service Methods (typical Grails patterns)
    // ========================================================================

    /**
     * Create order with items - demonstrates nested saves.
     */
    Order createOrderWithItems(Person customer, List<OrderItem> items) {
        withTransaction { ctx ->
            def order = new Order(
                orderNumber: Order.generateOrderNumber(),
                orderDate: new Date(),
                customer: customer
            )
            ctx.manage(order)

            items.each { item ->
                order.addToItems(item)
                ctx.manage(item)
            }

            if (!order.validate()) {
                throw new ValidationException("Order validation failed: ${order.errors}")
            }

            order.save()
            dynamicService?.orders?.add(order)
            order
        }
    }

    /**
     * Process payment - demonstrates transaction boundary.
     */
    Payment processPayment(Order order, BigDecimal amount, PaymentMethod method) {
        withTransaction { ctx ->
            def payment = new Payment(
                order: order,
                amount: amount,
                method: method,
                paymentDate: new Date(),
                transactionId: "TXN-${System.currentTimeMillis()}"
            )
            ctx.manage(payment)

            // Simulate external payment processing
            if (amount <= 0) {
                throw new PaymentException("Invalid payment amount")
            }

            payment.status = PaymentStatus.AUTHORIZED

            // Simulate capture
            payment.status = PaymentStatus.CAPTURED
            payment.save()

            // Update order status
            order.status = OrderStatus.CONFIRMED
            ctx.manage(order)
            order.save()

            payment
        }
    }

    /**
     * Batch update with transaction - common admin operation.
     */
    int batchUpdatePrices(String category, BigDecimal multiplier) {
        withTransaction { ctx ->
            def products = dynamicService?.products?.findAll { it.category?.name == category } ?: []

            int updated = 0
            products.each { product ->
                ctx.manage(product)
                product.price = product.price * multiplier
                if (product.save()) {
                    updated++
                }
            }
            updated
        }
    }

    /**
     * Complex operation with nested transactions.
     */
    Map transferInventory(Long fromWarehouseId, Long toWarehouseId, String productCode, int quantity) {
        withTransaction { outerCtx ->
            def result = [success: false, message: '']

            // Nested transaction for source warehouse
            withNewTransaction { srcCtx ->
                // Find source inventory
                def sourceInv = findInventory(fromWarehouseId, productCode)
                if (!sourceInv) {
                    result.message = "Source inventory not found"
                    return result
                }

                if (sourceInv.available < quantity) {
                    result.message = "Insufficient quantity"
                    return result
                }

                srcCtx.manage(sourceInv)
                sourceInv.quantityOnHand -= quantity
                sourceInv.save()
            }

            // Nested transaction for destination warehouse
            withNewTransaction { destCtx ->
                def destInv = findInventory(toWarehouseId, productCode)
                if (!destInv) {
                    // Create new inventory record
                    destInv = new Inventory(quantityOnHand: 0)
                }

                destCtx.manage(destInv)
                destInv.quantityOnHand += quantity
                destInv.save()
            }

            result.success = true
            result.message = "Transferred $quantity units"
            result
        }
    }

    private Inventory findInventory(Long warehouseId, String productCode) {
        // Simulated lookup
        null
    }

    /**
     * Read-only query with property access.
     */
    List<Map> getOrderSummaries() {
        withReadOnlyTransaction { ctx ->
            dynamicService?.orders?.collect { order ->
                [
                    id: order.id,
                    number: order.orderNumber,
                    customer: order.customer?.fullName,
                    items: order.items*.productName,
                    total: order.total,
                    status: order.status?.name()
                ]
            } ?: []
        }
    }

    /**
     * Service method with validation and error handling.
     */
    Person registerCustomer(Map params) {
        withTransaction { ctx ->
            // Check for existing email
            def existing = dynamicService?.findPersonByEmail(params.email)
            if (existing) {
                throw new DuplicateException("Email already registered")
            }

            def person = new Person(
                firstName: params.firstName,
                lastName: params.lastName,
                email: params.email,
                phone: params.phone
            )
            ctx.manage(person)

            if (params.address) {
                person.address = new Address(params.address)
            }

            if (!person.validate()) {
                throw new ValidationException("Validation failed: ${person.errors}")
            }

            person.save()
            dynamicService?.people?.add(person)
            person
        }
    }
}

/**
 * Transaction context for tracking managed entities.
 */
class TransactionContext {
    List<DomainObject> managedEntities = []
    boolean readOnly = false
    boolean committed = false
    boolean rolledBack = false
    TransactionContext parent

    void manage(DomainObject entity) {
        if (!managedEntities.contains(entity)) {
            managedEntities.add(entity)
        }
    }

    boolean isActive() {
        !committed && !rolledBack
    }
}

/**
 * Custom exceptions for service layer.
 */
class ValidationException extends RuntimeException {
    ValidationException(String message) { super(message) }
}

class PaymentException extends RuntimeException {
    PaymentException(String message) { super(message) }
}

class DuplicateException extends RuntimeException {
    DuplicateException(String message) { super(message) }
}
