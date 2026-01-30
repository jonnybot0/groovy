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
 * Base domain object simulating GORM entity behavior.
 * Provides dynamic property access, dirty tracking, and validation-like patterns.
 */
abstract class DomainObject {
    Long id
    Long version = 0L
    Date dateCreated
    Date lastUpdated

    // Dynamic properties storage (like GORM's dynamic attributes)
    protected Map<String, Object> dynamicProperties = [:]

    // Dirty tracking (simulates Hibernate dirty checking)
    protected Set<String> dirtyProperties = new HashSet<>()
    protected Map<String, Object> originalValues = [:]

    // Constraints-like validation errors
    protected List<String> errors = []

    /**
     * Dynamic property access - common in Grails views and controllers.
     */
    def propertyMissing(String name) {
        dynamicProperties[name]
    }

    def propertyMissing(String name, Object value) {
        if (!originalValues.containsKey(name)) {
            originalValues[name] = dynamicProperties[name]
        }
        dirtyProperties.add(name)
        dynamicProperties[name] = value
    }

    /**
     * Simulates GORM's isDirty() check.
     */
    boolean isDirty() {
        !dirtyProperties.isEmpty()
    }

    boolean isDirty(String propertyName) {
        dirtyProperties.contains(propertyName)
    }

    /**
     * Simulates GORM's getPersistentValue().
     */
    Object getPersistentValue(String propertyName) {
        originalValues.containsKey(propertyName) ? originalValues[propertyName] : this."$propertyName"
    }

    /**
     * Simulates GORM save() - exercises method dispatch and property access.
     */
    boolean save(Map args = [:]) {
        if (!validate()) {
            return false
        }

        if (id == null) {
            id = System.nanoTime()
            dateCreated = new Date()
        }
        lastUpdated = new Date()
        version++

        // Clear dirty tracking
        dirtyProperties.clear()
        originalValues.clear()

        if (args.flush) {
            // Simulate flush
            onFlush()
        }
        true
    }

    /**
     * Simulates validation.
     */
    boolean validate() {
        errors.clear()
        doValidate()
        errors.isEmpty()
    }

    protected void doValidate() {
        // Override in subclasses
    }

    protected void onFlush() {
        // Override in subclasses
    }

    /**
     * Simulates GORM's refresh().
     */
    void refresh() {
        dirtyProperties.clear()
        originalValues.clear()
    }

    /**
     * Simulates GORM's discard().
     */
    void discard() {
        // Restore original values
        originalValues.each { name, value ->
            if (dynamicProperties.containsKey(name)) {
                dynamicProperties[name] = value
            }
        }
        dirtyProperties.clear()
        originalValues.clear()
    }

    /**
     * Simulates GORM's toMap() - heavy property access.
     */
    Map toMap() {
        def result = [
            id: id,
            version: version,
            dateCreated: dateCreated,
            lastUpdated: lastUpdated
        ]
        result.putAll(dynamicProperties)
        result
    }

    /**
     * Simulates hasErrors().
     */
    boolean hasErrors() {
        !errors.isEmpty()
    }

    List<String> getErrors() {
        errors.asImmutable()
    }
}
