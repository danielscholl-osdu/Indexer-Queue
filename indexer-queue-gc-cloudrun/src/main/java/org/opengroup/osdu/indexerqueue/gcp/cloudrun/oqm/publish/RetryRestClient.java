/*
 *
 *  * Copyright 2022 Google LLC
 *  * Copyright 2022 EPAM Systems, Inc
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     https://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.opengroup.osdu.indexerqueue.gcp.cloudrun.oqm.publish;

import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.auth.TokenProvider;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@ConditionalOnExpression("'${index-task-type}'=='rest'")
@RequiredArgsConstructor
@Component
@Slf4j
public class RetryRestClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final TokenProvider tokenProvider;

    @Retryable(maxAttempts = 5, backoff = @Backoff(multiplier = 5))
    public ResponseEntity retryMessage(Object body, HttpHeaders httpHeaders, HttpMethod method, URI uri) {
        httpHeaders.set(DpsHeaders.AUTHORIZATION, "Bearer " + tokenProvider.getIdToken());
        RequestEntity<Object> requestEntity = new RequestEntity<>(body,
            httpHeaders, method,
            uri);
        return restTemplate.exchange(requestEntity, String.class);
    }

    @Recover
    public ResponseEntity writeLogOnFail(Exception e, RequestEntity httpsRequest) {
        log.error("Request: {} for Indexer have failed! With error: {}", httpsRequest, e);
        return null;
    }

}
