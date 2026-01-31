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
 * Service that implements GORM-like dynamic finders via methodMissing.
 * This exercises Groovy's dynamic method dispatch heavily.
 */
class DynamicFinderService {

    List<FinderPerson> people = []

    void addPerson(String firstName, String lastName, String email, int age) {
        people << new FinderPerson(
            firstName: firstName,
            lastName: lastName,
            email: email,
            age: age
        )
    }

    /**
     * methodMissing implements dynamic finders like:
     * - findByX(value)
     * - findAllByX(value)
     * - countByX(value)
     * - findByXAndY(value1, value2)
     * - findAllByXOrY(value1, value2)
     */
    def methodMissing(String name, args) {
        if (name.startsWith('findAllBy')) {
            return handleFindAllBy(name.substring(9), args)
        } else if (name.startsWith('findBy')) {
            return handleFindBy(name.substring(6), args)
        } else if (name.startsWith('countBy')) {
            return handleCountBy(name.substring(7), args)
        }
        throw new MissingMethodException(name, this.class, args as Object[])
    }

    private Object handleFindBy(String expression, args) {
        def predicate = buildPredicate(expression, args)
        people.find(predicate)
    }

    private List handleFindAllBy(String expression, args) {
        def predicate = buildPredicate(expression, args)
        people.findAll(predicate)
    }

    private int handleCountBy(String expression, args) {
        def predicate = buildPredicate(expression, args)
        people.count(predicate)
    }

    private Closure buildPredicate(String expression, args) {
        // Handle compound expressions: XAndY, XOrY
        if (expression.contains('And')) {
            def parts = expression.split('And')
            def prop1 = parts[0].uncapitalize()
            def prop2 = parts[1].uncapitalize()
            return { it."$prop1" == args[0] && it."$prop2" == args[1] }
        } else if (expression.contains('Or')) {
            def parts = expression.split('Or')
            def prop1 = parts[0].uncapitalize()
            def prop2 = parts[1].uncapitalize()
            return { it."$prop1" == args[0] || it."$prop2" == args[1] }
        } else {
            // Simple property
            def prop = expression.uncapitalize()
            return { it."$prop" == args[0] }
        }
    }

    // Java-style direct methods for baseline comparison
    FinderPerson javaFindByEmail(String email) {
        for (FinderPerson p : people) {
            if (email.equals(p.email)) {
                return p
            }
        }
        return null
    }

    List<FinderPerson> javaFindAllByLastName(String lastName) {
        List<FinderPerson> result = new ArrayList<>()
        for (FinderPerson p : people) {
            if (lastName.equals(p.lastName)) {
                result.add(p)
            }
        }
        return result
    }
}

/**
 * Simple person class for finder tests.
 */
class FinderPerson {
    String firstName
    String lastName
    String email
    int age

    String getFullName() { "$firstName $lastName" }
}
