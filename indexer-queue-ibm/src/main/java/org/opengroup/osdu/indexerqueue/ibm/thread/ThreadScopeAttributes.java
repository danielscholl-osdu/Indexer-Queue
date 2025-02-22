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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class ThreadScopeAttributes {

    protected final Map<String, Object> hBeans = new HashMap();
    protected final Map<String, Runnable> hRequestDestructionCallbacks = new LinkedHashMap();

    protected final Map<String, Object> getBeanMap() {
        return this.hBeans;
    }

    protected final void registerRequestDestructionCallback(
        @NonNull String name, @NonNull Runnable callback) {
        log.trace("Registering callback for: {} on runnable: {}", name, callback);
        this.hRequestDestructionCallbacks.put(name, callback);
    }

    public final void clear() {
        this.processDestructionCallbacks();
        this.hBeans.clear();
    }

    private void processDestructionCallbacks() {
        for (Map.Entry<String, Runnable> mapEntry : this.hRequestDestructionCallbacks.entrySet()) {
            Runnable callback = mapEntry.getValue();
            log.trace(
                "Performing destruction callback for: {} on thread: {}",
                mapEntry.getKey(),
                Thread.currentThread().getName());
            callback.run();
        }
        this.hRequestDestructionCallbacks.clear();
    }
}
