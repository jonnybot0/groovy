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
package org.apache.groovy.bench.grailslike.view

import org.apache.groovy.bench.grailslike.model.*

/**
 * Simulates GSP template rendering patterns.
 * Exercises heavy property access and GString interpolation.
 */
class TemplateSimulator {

    /**
     * Render person detail page.
     */
    String renderPersonDetail(Map model) {
        def person = model.person
        def orders = model.orders ?: []

        def sb = new StringBuilder()
        sb << "<div class='person-detail'>\n"
        sb << "  <h1>${person.fullName}</h1>\n"
        sb << "  <div class='info'>\n"
        sb << "    <p>Email: ${person.email}</p>\n"
        sb << "    <p>Phone: ${person.phone ?: 'N/A'}</p>\n"
        sb << "    <p>Age: ${person.age}</p>\n"

        if (person.address) {
            sb << "    <div class='address'>\n"
            sb << "      <h3>Address</h3>\n"
            sb << "      <p>${person.address.street}</p>\n"
            sb << "      <p>${person.address.city}, ${person.address.state} ${person.address.zipCode}</p>\n"
            sb << "    </div>\n"
        }

        sb << "  </div>\n"

        sb << "  <div class='orders'>\n"
        sb << "    <h2>Orders (${orders.size()})</h2>\n"
        if (orders) {
            sb << "    <ul>\n"
            orders.each { order ->
                sb << "      <li>${order.orderNumber} - ${order.status} - \$${order.total}</li>\n"
            }
            sb << "    </ul>\n"
        } else {
            sb << "    <p>No orders</p>\n"
        }
        sb << "  </div>\n"

        sb << "</div>\n"
        sb.toString()
    }

    /**
     * Render order detail page.
     */
    String renderOrderDetail(Map model) {
        def order = model.order
        def customer = model.customer ?: order?.customer
        def items = model.items ?: order?.items ?: []

        def sb = new StringBuilder()
        sb << "<div class='order-detail'>\n"
        sb << "  <h1>Order ${order.orderNumber}</h1>\n"
        sb << "  <div class='status ${order.status.name().toLowerCase()}'>${order.status}</div>\n"

        sb << "  <div class='customer'>\n"
        sb << "    <h3>Customer</h3>\n"
        sb << "    <p>${customer?.fullName ?: 'Unknown'}</p>\n"
        sb << "    <p>${customer?.email ?: ''}</p>\n"
        sb << "  </div>\n"

        sb << "  <div class='items'>\n"
        sb << "    <h3>Items (${items.size()})</h3>\n"
        sb << "    <table>\n"
        sb << "      <thead><tr><th>Product</th><th>Qty</th><th>Price</th><th>Total</th></tr></thead>\n"
        sb << "      <tbody>\n"
        items.each { item ->
            sb << "        <tr>\n"
            sb << "          <td>${item.productName}</td>\n"
            sb << "          <td>${item.quantity}</td>\n"
            sb << "          <td>\$${item.unitPrice}</td>\n"
            sb << "          <td>\$${item.lineTotal}</td>\n"
            sb << "        </tr>\n"
        }
        sb << "      </tbody>\n"
        sb << "    </table>\n"
        sb << "  </div>\n"

        sb << "  <div class='totals'>\n"
        sb << "    <p>Subtotal: \$${order.subtotal}</p>\n"
        sb << "    <p>Tax: \$${order.tax}</p>\n"
        sb << "    <p class='total'>Total: \$${order.total}</p>\n"
        sb << "  </div>\n"

        sb << "</div>\n"
        sb.toString()
    }

    /**
     * Render person list table.
     */
    String renderPersonList(Map model) {
        def people = model.personList ?: []
        def count = model.personCount ?: people.size()

        def sb = new StringBuilder()
        sb << "<div class='person-list'>\n"
        sb << "  <h1>People (${count})</h1>\n"
        sb << "  <table class='data-table'>\n"
        sb << "    <thead>\n"
        sb << "      <tr>\n"
        sb << "        <th>ID</th><th>Name</th><th>Email</th><th>Phone</th><th>City</th><th>Orders</th>\n"
        sb << "      </tr>\n"
        sb << "    </thead>\n"
        sb << "    <tbody>\n"

        people.each { person ->
            sb << "      <tr>\n"
            sb << "        <td>${person.id}</td>\n"
            sb << "        <td><a href='/person/show/${person.id}'>${person.fullName}</a></td>\n"
            sb << "        <td>${person.email}</td>\n"
            sb << "        <td>${person.phone ?: '-'}</td>\n"
            sb << "        <td>${person.address?.city ?: '-'}</td>\n"
            sb << "        <td>${person.orders?.size() ?: 0}</td>\n"
            sb << "      </tr>\n"
        }

        sb << "    </tbody>\n"
        sb << "  </table>\n"
        sb << "</div>\n"
        sb.toString()
    }

    /**
     * Render order list table.
     */
    String renderOrderList(Map model) {
        def orders = model.orderList ?: []

        def sb = new StringBuilder()
        sb << "<div class='order-list'>\n"
        sb << "  <h1>Orders (${orders.size()})</h1>\n"
        sb << "  <table class='data-table'>\n"
        sb << "    <thead>\n"
        sb << "      <tr>\n"
        sb << "        <th>Order #</th><th>Date</th><th>Customer</th><th>Items</th><th>Total</th><th>Status</th>\n"
        sb << "      </tr>\n"
        sb << "    </thead>\n"
        sb << "    <tbody>\n"

        orders.each { order ->
            sb << "      <tr class='status-${order.status.name().toLowerCase()}'>\n"
            sb << "        <td><a href='/order/show/${order.id}'>${order.orderNumber}</a></td>\n"
            sb << "        <td>${order.orderDate?.format('yyyy-MM-dd')}</td>\n"
            sb << "        <td>${order.customer?.fullName ?: 'Unknown'}</td>\n"
            sb << "        <td>${order.itemCount}</td>\n"
            sb << "        <td>\$${order.total}</td>\n"
            sb << "        <td>${order.status}</td>\n"
            sb << "      </tr>\n"
        }

        sb << "    </tbody>\n"
        sb << "  </table>\n"
        sb << "</div>\n"
        sb.toString()
    }

    /**
     * Render dashboard with multiple sections.
     */
    String renderDashboard(Map model) {
        def recentOrders = model.recentOrders ?: []
        def stats = model.stats ?: [:]
        def topCustomers = model.topCustomers ?: []

        def sb = new StringBuilder()
        sb << "<div class='dashboard'>\n"

        // Stats cards
        sb << "  <div class='stats-row'>\n"
        stats.each { key, value ->
            sb << "    <div class='stat-card'>\n"
            sb << "      <h4>${key.replaceAll(/([A-Z])/, ' $1').trim()}</h4>\n"
            sb << "      <p class='value'>${value}</p>\n"
            sb << "    </div>\n"
        }
        sb << "  </div>\n"

        // Top customers
        sb << "  <div class='top-customers'>\n"
        sb << "    <h3>Top Customers</h3>\n"
        sb << "    <table>\n"
        topCustomers.each { customer ->
            sb << "      <tr>\n"
            sb << "        <td>${customer.customerName}</td>\n"
            sb << "        <td>\$${customer.totalSpent}</td>\n"
            sb << "        <td>${customer.orderCount} orders</td>\n"
            sb << "      </tr>\n"
        }
        sb << "    </table>\n"
        sb << "  </div>\n"

        // Recent orders
        sb << "  <div class='recent-orders'>\n"
        sb << "    <h3>Recent Orders</h3>\n"
        sb << "    <ul>\n"
        recentOrders.each { order ->
            sb << "      <li>\n"
            sb << "        <span class='order-number'>${order.orderNumber}</span>\n"
            sb << "        <span class='customer'>${order.customer?.fullName}</span>\n"
            sb << "        <span class='total'>\$${order.total}</span>\n"
            sb << "        <span class='status ${order.status.name().toLowerCase()}'>${order.status}</span>\n"
            sb << "      </li>\n"
        }
        sb << "    </ul>\n"
        sb << "  </div>\n"

        sb << "</div>\n"
        sb.toString()
    }

    /**
     * Render order with line items (nested iteration).
     */
    String renderOrderWithItems(Map model) {
        def order = model.order
        def items = model.items ?: order?.items ?: []

        def sb = new StringBuilder()
        sb << "<div class='order-items'>\n"
        sb << "  <h2>Order ${order.orderNumber}</h2>\n"

        items.eachWithIndex { item, idx ->
            sb << "  <div class='item' data-index='${idx}'>\n"
            sb << "    <div class='item-header'>\n"
            sb << "      <span class='code'>${item.productCode}</span>\n"
            sb << "      <span class='name'>${item.productName}</span>\n"
            sb << "    </div>\n"
            sb << "    <div class='item-details'>\n"
            sb << "      <span class='qty'>Qty: ${item.quantity}</span>\n"
            sb << "      <span class='price'>Price: \$${item.unitPrice}</span>\n"
            if (item.hasDiscount) {
                sb << "      <span class='discount'>Discount: ${item.discount * 100}%</span>\n"
                sb << "      <span class='savings'>Savings: \$${item.savings}</span>\n"
            }
            sb << "      <span class='line-total'>Total: \$${item.lineTotal}</span>\n"
            sb << "    </div>\n"
            sb << "  </div>\n"
        }

        sb << "  <div class='order-totals'>\n"
        sb << "    <div>Subtotal: \$${order.subtotal}</div>\n"
        sb << "    <div>Tax (${order.taxRate * 100}%): \$${order.tax}</div>\n"
        sb << "    <div class='grand-total'>Total: \$${order.total}</div>\n"
        sb << "  </div>\n"
        sb << "</div>\n"
        sb.toString()
    }

    /**
     * Render using spread operator.
     */
    String renderWithSpreadOperator(Map model) {
        def people = model.personList ?: []

        def sb = new StringBuilder()
        sb << "<div class='spread-demo'>\n"

        // Spread operator for names
        def names = people*.fullName
        sb << "  <div class='names'>\n"
        names.each { name ->
            sb << "    <span>${name}</span>\n"
        }
        sb << "  </div>\n"

        // Spread operator for emails
        def emails = people*.email
        sb << "  <div class='emails'>\n"
        emails.findAll { it != null }.each { email ->
            sb << "    <a href='mailto:${email}'>${email}</a>\n"
        }
        sb << "  </div>\n"

        // Spread on nested properties
        def cities = people*.address*.city
        sb << "  <div class='cities'>\n"
        cities.findAll { it != null }.unique().each { city ->
            sb << "    <span>${city}</span>\n"
        }
        sb << "  </div>\n"

        sb << "</div>\n"
        sb.toString()
    }

    /**
     * Render with conditional logic.
     */
    String renderConditional(Map model) {
        def order = model?.order

        def sb = new StringBuilder()
        sb << "<div class='conditional-demo'>\n"

        if (order) {
            sb << "  <div class='order-status'>\n"

            if (order.status == OrderStatus.PENDING) {
                sb << "    <div class='alert warning'>Order is pending processing</div>\n"
            } else if (order.status == OrderStatus.CONFIRMED) {
                sb << "    <div class='alert info'>Order has been confirmed</div>\n"
            } else if (order.status == OrderStatus.SHIPPED) {
                sb << "    <div class='alert success'>Order has shipped!</div>\n"
                if (order.dynamicProperties['trackingNumber']) {
                    sb << "    <p>Tracking: ${order.dynamicProperties['trackingNumber']}</p>\n"
                }
            } else if (order.status == OrderStatus.COMPLETED) {
                sb << "    <div class='alert success'>Order completed</div>\n"
            } else if (order.status == OrderStatus.CANCELLED) {
                sb << "    <div class='alert error'>Order was cancelled</div>\n"
            }

            if (order.canCancel()) {
                sb << "    <button class='btn-cancel'>Cancel Order</button>\n"
            }

            if (order.total > 100) {
                sb << "    <div class='free-shipping'>Qualifies for free shipping!</div>\n"
            }

            sb << "  </div>\n"
        } else {
            sb << "  <div class='no-order'>No order to display</div>\n"
        }

        sb << "</div>\n"
        sb.toString()
    }

    /**
     * Heavy GString interpolation.
     */
    String renderGStringHeavy(Map model) {
        def person = model.person

        def sb = new StringBuilder()

        // Multiple interpolations per line
        sb << "Name: ${person.firstName} ${person.lastName} (${person.fullName})\n"
        sb << "Contact: ${person.email ?: 'no email'} | ${person.phone ?: 'no phone'}\n"

        if (person.address) {
            def addr = person.address
            sb << "Address: ${addr.street}, ${addr.city}, ${addr.state} ${addr.zipCode}, ${addr.country}\n"
            sb << "Formatted: ${addr.formatted}\n"
        }

        sb << "Orders: ${person.orders?.size() ?: 0} total\n"

        person.orders?.each { order ->
            sb << "  - Order ${order.orderNumber}: ${order.itemCount} items, \$${order.total} (${order.status})\n"
            order.items?.each { item ->
                sb << "    * ${item.productCode}: ${item.productName} x${item.quantity} @ \$${item.unitPrice} = \$${item.lineTotal}\n"
            }
        }

        def totalSpent = person.orders?.sum { it.total } ?: 0
        sb << "Total spent: \$${totalSpent}\n"

        sb.toString()
    }

    /**
     * Full page render with layout.
     */
    String renderFullPage(Map model) {
        def title = model.title ?: "Dashboard"

        def sb = new StringBuilder()

        // Layout start
        sb << "<!DOCTYPE html>\n"
        sb << "<html>\n"
        sb << "<head>\n"
        sb << "  <title>${title}</title>\n"
        sb << "  <meta charset='utf-8'>\n"
        sb << "</head>\n"
        sb << "<body>\n"

        // Header
        sb << "<header>\n"
        sb << "  <nav>\n"
        sb << "    <a href='/'>Home</a>\n"
        sb << "    <a href='/person/list'>People</a>\n"
        sb << "    <a href='/order/list'>Orders</a>\n"
        sb << "    <a href='/dashboard'>Dashboard</a>\n"
        sb << "  </nav>\n"
        sb << "</header>\n"

        // Main content
        sb << "<main>\n"
        sb << renderDashboard(model)
        sb << "</main>\n"

        // Footer
        sb << "<footer>\n"
        sb << "  <p>Generated at ${new Date()}</p>\n"
        sb << "  <p>Stats: ${model.stats?.totalOrders ?: 0} orders, "
        sb << "\$${model.stats?.totalRevenue ?: 0} revenue</p>\n"
        sb << "</footer>\n"

        sb << "</body>\n"
        sb << "</html>\n"
        sb.toString()
    }
}
