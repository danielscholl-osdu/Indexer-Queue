/*
 * Copyright 2020-2022 Google LLC
 * Copyright 2020-2022 EPAM Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.indexerqueue.gcp.cloudrun.oqm.publish;

import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@ConditionalOnExpression("'${index-task-type}'=='rest'")
@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncProxyUtil {

    private final RetryRestClient retryRestClient;

    @Async
    public void asyncTask(Object body, HttpHeaders httpHeaders, HttpMethod method, URI uri) {
        ResponseEntity response = retryRestClient.retryMessage(body, httpHeaders, method, uri);
        log.info("RestMessagePublisher received response: {}", response);
    }
}
