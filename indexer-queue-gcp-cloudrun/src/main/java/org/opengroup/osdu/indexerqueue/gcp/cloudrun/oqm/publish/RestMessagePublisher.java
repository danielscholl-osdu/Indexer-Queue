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

import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.CloudTaskRequest;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.indexerqueue.gcp.cloudrun.config.IndexerQueueConfigProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * RestMessagePublisher supports PUSH approach, sends incoming OqmMessages
 * with indexing tasks to Indexer REST API.
 */
@ConditionalOnExpression("'${index-task-type}'=='rest'")
@RequiredArgsConstructor
@Component
@Slf4j
public class RestMessagePublisher implements MessagePublisher {

  private final RestTemplate restTemplate = new RestTemplate();
  private final IndexerQueueConfigProperties properties;

  @Retryable(maxAttempts = 5, backoff = @Backoff(multiplier = 5))
  @Override
  public HttpStatus sendMessage(CloudTaskRequest request, DpsHeaders dpsHeaders) {
    RecordChangedMessages messages = RecordChangedMessages.builder()
        .messageId(dpsHeaders.getCorrelationId())
        .data(request.getMessage())
        .attributes(getHttpHeaders(dpsHeaders).toSingleValueMap())
        .publishTime(LocalDateTime.now().toString())
        .build();
    RequestEntity<RecordChangedMessages> httpRequest = new RequestEntity<>(messages,
        getHttpHeaders(dpsHeaders), HttpMethod.POST,
        getUri(properties.getIndexerHost() + request.getUrl()));
    log.info("RestMessagePublisher sending request: {}", httpRequest);
    ResponseEntity<HttpStatus> response = restTemplate.exchange(httpRequest, HttpStatus.class);
    log.info("RestMessagePublisher received response: {}", response);
    return response.getStatusCode();
  }

  @NotNull
  private HttpHeaders getHttpHeaders(DpsHeaders headers) {
    HttpHeaders headersMap = new HttpHeaders();
    headersMap.set("account-id", headers.getAccountId());
    headersMap.set("data-partition-id", headers.getPartitionId());
    headersMap.set("correlation-id", headers.getCorrelationId());
    headersMap.set("user", headers.getUserEmail());
    headersMap.set("authorization", headers.getAuthorization());
    headersMap.set("x-cloudtasks-queuename", headers.getPartitionId());
    headersMap.setContentType(MediaType.APPLICATION_JSON);
    return headersMap;
  }

  private URI getUri(String url) {
    try {
      return new URI(url);
    } catch (URISyntaxException e) {
      throw new AppException(SC_INTERNAL_SERVER_ERROR, e.getReason(), e.getMessage());
    }
  }
}
