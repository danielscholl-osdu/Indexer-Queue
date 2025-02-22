/*
 *  Copyright 2020-2022 Google LLC
 *  Copyright 2020-2022 EPAM Systems, Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.indexerqueue.ibm.thread;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;

import java.util.Map;

@Slf4j
public class ThreadScope implements Scope {

  public Object get(String name, ObjectFactory<?> factory) {
    log.trace("Get bean:{} with factory: {} current Thread: {}", name, factory,
        Thread.currentThread().getName());
    Object result = null;
    Map<String, Object> hBeans = ThreadScopeContextHolder.currentThreadScopeAttributes()
        .getBeanMap();
    if (!hBeans.containsKey(name)) {
      result = factory.getObject();
      log.trace(
          "No bean in context with name: {} factory provisioning result is: {} current Thread: {}",
          name, result, Thread.currentThread().getName());
      hBeans.put(name, result);
    } else {
      result = hBeans.get(name);
    }

    return result;
  }

  public Object remove(String name) {
    log.trace("Removing bean : {} current Thread: {}", name, Thread.currentThread().getName());
    Object result = null;
    Map<String, Object> hBeans = ThreadScopeContextHolder.currentThreadScopeAttributes()
        .getBeanMap();
    if (hBeans.containsKey(name)) {
      result = hBeans.get(name);
      hBeans.remove(name);
    }

    return result;
  }

  public void registerDestructionCallback(String name, Runnable callback) {
    ThreadScopeContextHolder.currentThreadScopeAttributes().registerRequestDestructionCallback(name, callback);
  }

  public Object resolveContextualObject(String key) {
    return null;
  }

  public String getConversationId() {
    return Thread.currentThread().getName();
  }
}

