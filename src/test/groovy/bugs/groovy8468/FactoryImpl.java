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
package bugs.groovy8468;

public class FactoryImpl implements Factory {
  @Override @SuppressWarnings("unchecked")
  public <T extends Face> T[] makeArray(Class<T> clazz) {
    return (T[]) java.lang.reflect.Array.newInstance(clazz, 0);
  }

  /*public static void main(String[] args) {
    Factory factory = new FactoryImpl();
    Face[] array = factory.makeArray(FaceImpl.class);
    System.out.println(array);
  }*/
}
