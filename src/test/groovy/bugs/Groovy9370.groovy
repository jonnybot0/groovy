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
package bugs

import groovy.transform.CompileStatic
import org.junit.Test

import static groovy.test.GroovyAssert.assertScript

@CompileStatic
final class Groovy9370 {

    @Test
    void testClosureForSAMWithinAIC() {
        assertScript '''
            class Main {
                static final Pogo pogo = new Pogo()

                @groovy.transform.CompileStatic
                static main(args) {
                    def face = new Face() {
                        @Override
                        def meth() {
                            pogo.thing1 { ->
                                pogo.thing2() // STC error; AIC's propertyMissing is taking precedence
                            }
                        }
                    }
                    assert face.meth() == 'works'
                }
            }

            @FunctionalInterface
            interface Face {
                def meth()
            }

            class Pogo {
                def thing1(Face face) {
                    face.meth()
                }

                def thing2() {
                    'works'
                }
            }
        '''
    }
}
