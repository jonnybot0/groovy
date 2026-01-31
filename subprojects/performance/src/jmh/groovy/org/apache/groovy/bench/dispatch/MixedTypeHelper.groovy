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
package org.apache.groovy.bench.dispatch

import org.openjdk.jmh.infra.Blackhole

/**
 * Helper for mixed type collection benchmarks.
 */
class MixedTypeHelper {

    // All type classes implement this interface for Java baseline
    static interface Nameable {
        String getName()
        int getValue()
        Object process()
    }

    // Generate 20 different classes
    static final List<Class> TYPE_CLASSES = [
        MixedType01, MixedType02, MixedType03, MixedType04, MixedType05,
        MixedType06, MixedType07, MixedType08, MixedType09, MixedType10,
        MixedType11, MixedType12, MixedType13, MixedType14, MixedType15,
        MixedType16, MixedType17, MixedType18, MixedType19, MixedType20
    ]

    /**
     * Create a collection with N different types.
     */
    static List<Object> createMixedCollection(int size, int numTypes) {
        def typesToUse = TYPE_CLASSES.take(numTypes)
        def result = []
        size.times { i ->
            def typeClass = typesToUse[i % numTypes]
            result << typeClass.newInstance([name: "Item$i", value: i])
        }
        result
    }

    /**
     * Call getName() on all objects (exercises callsite cache).
     */
    static void callGetName(List objects, Blackhole bh) {
        for (obj in objects) {
            bh.consume(obj.getName())
        }
    }

    /**
     * Call getValue() on all objects.
     */
    static void callGetValue(List objects, Blackhole bh) {
        for (obj in objects) {
            bh.consume(obj.getValue())
        }
    }

    /**
     * Call process() on all objects.
     */
    static void callProcess(List objects, Blackhole bh) {
        for (obj in objects) {
            bh.consume(obj.process())
        }
    }

    /**
     * Collect names using spread operator.
     */
    static List collectNames(List objects) {
        objects*.getName()
    }

    /**
     * Java interface call for baseline.
     */
    static void javaInterfaceCall(List objects, Blackhole bh) {
        for (Object obj : objects) {
            if (obj instanceof Nameable) {
                bh.consume(((Nameable) obj).getName())
            }
        }
    }
}

// 20 different classes with same interface
class MixedType01 implements MixedTypeHelper.Nameable {
    String name; int value
    String getName() { name }
    int getValue() { value }
    Object process() { "01:$name" }
}

class MixedType02 implements MixedTypeHelper.Nameable {
    String name; int value
    String getName() { name }
    int getValue() { value }
    Object process() { "02:$name" }
}

class MixedType03 implements MixedTypeHelper.Nameable {
    String name; int value
    String getName() { name }
    int getValue() { value }
    Object process() { "03:$name" }
}

class MixedType04 implements MixedTypeHelper.Nameable {
    String name; int value
    String getName() { name }
    int getValue() { value }
    Object process() { "04:$name" }
}

class MixedType05 implements MixedTypeHelper.Nameable {
    String name; int value
    String getName() { name }
    int getValue() { value }
    Object process() { "05:$name" }
}

class MixedType06 implements MixedTypeHelper.Nameable {
    String name; int value
    String getName() { name }
    int getValue() { value }
    Object process() { "06:$name" }
}

class MixedType07 implements MixedTypeHelper.Nameable {
    String name; int value
    String getName() { name }
    int getValue() { value }
    Object process() { "07:$name" }
}

class MixedType08 implements MixedTypeHelper.Nameable {
    String name; int value
    String getName() { name }
    int getValue() { value }
    Object process() { "08:$name" }
}

class MixedType09 implements MixedTypeHelper.Nameable {
    String name; int value
    String getName() { name }
    int getValue() { value }
    Object process() { "09:$name" }
}

class MixedType10 implements MixedTypeHelper.Nameable {
    String name; int value
    String getName() { name }
    int getValue() { value }
    Object process() { "10:$name" }
}

class MixedType11 implements MixedTypeHelper.Nameable {
    String name; int value
    String getName() { name }
    int getValue() { value }
    Object process() { "11:$name" }
}

class MixedType12 implements MixedTypeHelper.Nameable {
    String name; int value
    String getName() { name }
    int getValue() { value }
    Object process() { "12:$name" }
}

class MixedType13 implements MixedTypeHelper.Nameable {
    String name; int value
    String getName() { name }
    int getValue() { value }
    Object process() { "13:$name" }
}

class MixedType14 implements MixedTypeHelper.Nameable {
    String name; int value
    String getName() { name }
    int getValue() { value }
    Object process() { "14:$name" }
}

class MixedType15 implements MixedTypeHelper.Nameable {
    String name; int value
    String getName() { name }
    int getValue() { value }
    Object process() { "15:$name" }
}

class MixedType16 implements MixedTypeHelper.Nameable {
    String name; int value
    String getName() { name }
    int getValue() { value }
    Object process() { "16:$name" }
}

class MixedType17 implements MixedTypeHelper.Nameable {
    String name; int value
    String getName() { name }
    int getValue() { value }
    Object process() { "17:$name" }
}

class MixedType18 implements MixedTypeHelper.Nameable {
    String name; int value
    String getName() { name }
    int getValue() { value }
    Object process() { "18:$name" }
}

class MixedType19 implements MixedTypeHelper.Nameable {
    String name; int value
    String getName() { name }
    int getValue() { value }
    Object process() { "19:$name" }
}

class MixedType20 implements MixedTypeHelper.Nameable {
    String name; int value
    String getName() { name }
    int getValue() { value }
    Object process() { "20:$name" }
}
