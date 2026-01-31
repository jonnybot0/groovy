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
 * Additional domain classes to simulate a realistic Grails application
 * with many different entity types (polymorphic dispatch scenarios).
 */

// ============================================================================
// User/Security Domain Classes
// ============================================================================

class User extends DomainObject {
    String username
    String passwordHash
    String email
    boolean enabled = true
    boolean accountLocked = false
    Date lastLogin
    List<Role> roles = []

    String getDisplayName() { username }
    boolean hasRole(String roleName) { roles.any { it.authority == roleName } }

    @Override
    Map toMap() {
        super.toMap() + [username: username, email: email, enabled: enabled, roles: roles*.authority]
    }
}

class Role extends DomainObject {
    String authority
    String description

    @Override
    Map toMap() { super.toMap() + [authority: authority, description: description] }
}

class AuditLog extends DomainObject {
    String action
    String entityType
    Long entityId
    String username
    String ipAddress
    Date timestamp = new Date()
    String details

    @Override
    Map toMap() {
        super.toMap() + [action: action, entityType: entityType, entityId: entityId,
                         username: username, ipAddress: ipAddress, details: details]
    }
}

// ============================================================================
// Product/Inventory Domain Classes
// ============================================================================

class Product extends DomainObject {
    String sku
    String name
    String description
    BigDecimal price
    BigDecimal cost
    Category category
    List<ProductAttribute> attributes = []
    boolean active = true

    BigDecimal getMargin() { price - cost }
    BigDecimal getMarginPercent() { cost > 0 ? (margin / cost) * 100 : 0 }

    @Override
    Map toMap() {
        super.toMap() + [sku: sku, name: name, price: price, margin: margin,
                         category: category?.name, attributes: attributes*.toMap()]
    }
}

class Category extends DomainObject {
    String name
    String code
    Category parent
    List<Category> children = []
    int sortOrder = 0

    String getFullPath() {
        parent ? "${parent.fullPath} > $name" : name
    }

    @Override
    Map toMap() { super.toMap() + [name: name, code: code, fullPath: fullPath] }
}

class ProductAttribute extends DomainObject {
    String name
    String value
    String type = 'STRING'
    Product product

    @Override
    Map toMap() { super.toMap() + [name: name, value: value, type: type] }
}

class Inventory extends DomainObject {
    Product product
    Warehouse warehouse
    int quantityOnHand = 0
    int quantityReserved = 0
    int reorderPoint = 10
    int reorderQuantity = 50

    int getAvailable() { quantityOnHand - quantityReserved }
    boolean needsReorder() { available <= reorderPoint }

    @Override
    Map toMap() {
        super.toMap() + [productId: product?.id, warehouseId: warehouse?.id,
                         onHand: quantityOnHand, available: available, needsReorder: needsReorder()]
    }
}

class Warehouse extends DomainObject {
    String code
    String name
    Address address
    boolean active = true

    @Override
    Map toMap() { super.toMap() + [code: code, name: name, address: address?.toMap()] }
}

// ============================================================================
// Customer/CRM Domain Classes
// ============================================================================

class Customer extends Person {
    String customerNumber
    CustomerType customerType = CustomerType.INDIVIDUAL
    BigDecimal creditLimit = BigDecimal.ZERO
    BigDecimal balance = BigDecimal.ZERO
    String notes
    List<Contact> contacts = []

    BigDecimal getAvailableCredit() { creditLimit - balance }

    @Override
    Map toMap() {
        super.toMap() + [customerNumber: customerNumber, customerType: customerType?.name(),
                         creditLimit: creditLimit, balance: balance, availableCredit: availableCredit]
    }
}

enum CustomerType {
    INDIVIDUAL, BUSINESS, WHOLESALE, VIP
}

class Contact extends DomainObject {
    String firstName
    String lastName
    String email
    String phone
    String title
    boolean isPrimary = false
    Customer customer

    String getFullName() { "$firstName $lastName" }

    @Override
    Map toMap() { super.toMap() + [fullName: fullName, email: email, phone: phone, isPrimary: isPrimary] }
}

// ============================================================================
// Shipping/Logistics Domain Classes
// ============================================================================

class Shipment extends DomainObject {
    Order order
    String trackingNumber
    String carrier
    ShipmentStatus status = ShipmentStatus.PENDING
    Date shipDate
    Date deliveryDate
    BigDecimal weight
    BigDecimal shippingCost
    Address fromAddress
    Address toAddress
    List<ShipmentItem> items = []

    int getTotalQuantity() { items.sum { it.quantity } ?: 0 }

    @Override
    Map toMap() {
        super.toMap() + [trackingNumber: trackingNumber, carrier: carrier, status: status?.name(),
                         totalQuantity: totalQuantity, shippingCost: shippingCost]
    }
}

enum ShipmentStatus {
    PENDING, PICKED, PACKED, SHIPPED, IN_TRANSIT, DELIVERED, RETURNED
}

class ShipmentItem extends DomainObject {
    Shipment shipment
    OrderItem orderItem
    int quantity
    String lotNumber

    @Override
    Map toMap() { super.toMap() + [orderItemId: orderItem?.id, quantity: quantity, lotNumber: lotNumber] }
}

// ============================================================================
// Payment/Financial Domain Classes
// ============================================================================

class Payment extends DomainObject {
    Order order
    BigDecimal amount
    PaymentMethod method
    PaymentStatus status = PaymentStatus.PENDING
    String transactionId
    Date paymentDate
    String notes

    @Override
    Map toMap() {
        super.toMap() + [orderId: order?.id, amount: amount, method: method?.name(),
                         status: status?.name(), transactionId: transactionId]
    }
}

enum PaymentMethod {
    CREDIT_CARD, DEBIT_CARD, PAYPAL, BANK_TRANSFER, CHECK, CASH
}

enum PaymentStatus {
    PENDING, AUTHORIZED, CAPTURED, REFUNDED, FAILED, CANCELLED
}

class Invoice extends DomainObject {
    String invoiceNumber
    Order order
    Customer customer
    Date invoiceDate
    Date dueDate
    BigDecimal subtotal
    BigDecimal tax
    BigDecimal total
    InvoiceStatus status = InvoiceStatus.DRAFT
    List<InvoiceLine> lines = []

    boolean isOverdue() { status == InvoiceStatus.SENT && new Date() > dueDate }

    @Override
    Map toMap() {
        super.toMap() + [invoiceNumber: invoiceNumber, total: total, status: status?.name(), isOverdue: isOverdue()]
    }
}

enum InvoiceStatus {
    DRAFT, SENT, PAID, OVERDUE, CANCELLED, VOID
}

class InvoiceLine extends DomainObject {
    Invoice invoice
    String description
    int quantity
    BigDecimal unitPrice
    BigDecimal lineTotal

    @Override
    Map toMap() { super.toMap() + [description: description, quantity: quantity, lineTotal: lineTotal] }
}

// ============================================================================
// Content/CMS Domain Classes
// ============================================================================

class Article extends DomainObject {
    String title
    String slug
    String content
    String summary
    User author
    ArticleStatus status = ArticleStatus.DRAFT
    Date publishDate
    List<Tag> tags = []
    List<Comment> comments = []

    int getWordCount() { content?.split(/\s+/)?.size() ?: 0 }
    int getCommentCount() { comments.size() }

    @Override
    Map toMap() {
        super.toMap() + [title: title, slug: slug, author: author?.username,
                         status: status?.name(), wordCount: wordCount, commentCount: commentCount]
    }
}

enum ArticleStatus {
    DRAFT, REVIEW, PUBLISHED, ARCHIVED
}

class Tag extends DomainObject {
    String name
    String slug
    int usageCount = 0

    @Override
    Map toMap() { super.toMap() + [name: name, slug: slug, usageCount: usageCount] }
}

class Comment extends DomainObject {
    Article article
    User author
    String content
    boolean approved = false
    Comment parent
    List<Comment> replies = []

    int getReplyCount() { replies.size() }

    @Override
    Map toMap() {
        super.toMap() + [author: author?.username, content: content?.take(100),
                         approved: approved, replyCount: replyCount]
    }
}

// ============================================================================
// Configuration/Settings Domain Classes
// ============================================================================

class Setting extends DomainObject {
    String key
    String value
    String type = 'STRING'
    String description
    boolean encrypted = false

    Object getParsedValue() {
        switch (type) {
            case 'INTEGER': return value?.toInteger()
            case 'BOOLEAN': return value?.toBoolean()
            case 'DECIMAL': return value ? new BigDecimal(value) : null
            default: return value
        }
    }

    @Override
    Map toMap() { super.toMap() + [key: key, value: encrypted ? '***' : value, type: type] }
}

class Notification extends DomainObject {
    User user
    String title
    String message
    NotificationType type
    boolean read = false
    Date readAt
    String actionUrl

    @Override
    Map toMap() {
        super.toMap() + [title: title, type: type?.name(), read: read, actionUrl: actionUrl]
    }
}

enum NotificationType {
    INFO, SUCCESS, WARNING, ERROR, ALERT
}

// ============================================================================
// Reporting/Analytics Domain Classes
// ============================================================================

class Report extends DomainObject {
    String name
    String description
    String query
    ReportType type
    User owner
    boolean shared = false
    Map<String, Object> parameters = [:]

    @Override
    Map toMap() { super.toMap() + [name: name, type: type?.name(), shared: shared] }
}

enum ReportType {
    TABLE, CHART, DASHBOARD, EXPORT
}

class ScheduledTask extends DomainObject {
    String name
    String cronExpression
    String taskClass
    boolean enabled = true
    Date lastRun
    Date nextRun
    String lastResult
    int runCount = 0
    int failCount = 0

    double getSuccessRate() { runCount > 0 ? ((runCount - failCount) / runCount) * 100 : 0 }

    @Override
    Map toMap() {
        super.toMap() + [name: name, enabled: enabled, lastRun: lastRun, successRate: successRate]
    }
}
