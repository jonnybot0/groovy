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
 * Person domain object simulating a typical Grails domain class.
 * Has relationships to Order (hasMany pattern).
 */
class Person extends DomainObject {
    String firstName
    String lastName
    String email
    Date birthDate
    String phone
    Address address

    // hasMany relationship simulation
    List<Order> orders = []

    // Computed property (common in Grails)
    String getFullName() {
        "$firstName $lastName"
    }

    // Another computed property
    int getAge() {
        if (birthDate == null) return 0
        def now = Calendar.getInstance()
        def birth = Calendar.getInstance()
        birth.time = birthDate
        int age = now.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
        if (now.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) {
            age--
        }
        age
    }

    // Simulates GORM's addToOrders()
    Person addToOrders(Order order) {
        order.customer = this
        orders.add(order)
        this
    }

    // Simulates GORM's removeFromOrders()
    Person removeFromOrders(Order order) {
        orders.remove(order)
        order.customer = null
        this
    }

    @Override
    protected void doValidate() {
        if (!firstName?.trim()) {
            errors.add("firstName cannot be blank")
        }
        if (!lastName?.trim()) {
            errors.add("lastName cannot be blank")
        }
        if (email && !email.contains('@')) {
            errors.add("email is not valid")
        }
    }

    @Override
    Map toMap() {
        def result = super.toMap()
        result.putAll([
            firstName: firstName,
            lastName: lastName,
            fullName: fullName,
            email: email,
            birthDate: birthDate,
            age: age,
            phone: phone,
            address: address?.toMap(),
            orderCount: orders.size()
        ])
        result
    }

    /**
     * Simulates dynamic finder: findByEmail
     */
    static Person findByEmail(List<Person> people, String email) {
        people.find { it.email == email }
    }

    /**
     * Simulates dynamic finder: findAllByLastName
     */
    static List<Person> findAllByLastName(List<Person> people, String lastName) {
        people.findAll { it.lastName == lastName }
    }

    /**
     * Simulates criteria query
     */
    static List<Person> findAllWithOrders(List<Person> people) {
        people.findAll { !it.orders.isEmpty() }
    }
}

/**
 * Embedded Address class (like Grails embedded objects).
 */
class Address {
    String street
    String city
    String state
    String zipCode
    String country = "USA"

    String getFormatted() {
        "$street, $city, $state $zipCode, $country"
    }

    Map toMap() {
        [
            street: street,
            city: city,
            state: state,
            zipCode: zipCode,
            country: country,
            formatted: formatted
        ]
    }
}
