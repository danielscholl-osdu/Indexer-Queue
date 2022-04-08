/*
 * Copyright 2022 Google LLC
 * Copyright 2022 EPAM Systems, Inc
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

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.CloudTaskRequest;
import org.opengroup.osdu.core.gcp.oqm.driver.OqmDriver;
import org.opengroup.osdu.core.gcp.oqm.model.OqmDestination;
import org.opengroup.osdu.core.gcp.oqm.model.OqmMessage;
import org.opengroup.osdu.core.gcp.oqm.model.OqmTopic;
import org.opengroup.osdu.indexerqueue.gcp.cloudrun.config.IndexerQueueConfigProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@ConditionalOnExpression("'${index-task-type}'=='queue' and '${oqmDriver}'=='rabbitmq'")
@Slf4j
@RequiredArgsConstructor
@Component
public class BrokerMessagePublisher implements MessagePublisher {

  private final OqmDriver driver;
  private final IndexerQueueConfigProperties config;

  @Override
  public HttpStatus sendMessage(CloudTaskRequest request, DpsHeaders headers) {
    try {
      driver.publish(getMessage(request, headers), getTopic(), getDestination(headers.getPartitionId()));
    } catch (Exception e) {
      log.info(e.getMessage());
      return HttpStatus.INTERNAL_SERVER_ERROR;
    }
    log.info("OQM message published: {}", request.getMessage());
    return HttpStatus.OK;
  }

  private OqmMessage getMessage(CloudTaskRequest request, DpsHeaders headers) {
    return OqmMessage.builder()
        .attributes(getAttributes(request, headers))
        .data(request.getMessage())
        .build();
  }

  /**
   * Fill attributes for OqmMessage with `relative-indexer-worker-url` responsible for
   * defining index or reindex task type and headers like `data-partition-id` or `authorization`.
   */
  @NotNull
  private Map<String, String> getAttributes(CloudTaskRequest request, DpsHeaders headers) {
    Map<String, String> attributes = headers.getHeaders();
    attributes.put("relative-indexer-worker-url", request.getUrl());
    return attributes;
  }

  private OqmTopic getTopic() {
    return OqmTopic.builder()
        .name(config.getRecordsTopicName())
        .build();
  }

  private OqmDestination getDestination(String partitionId) {
    return OqmDestination.builder()
        .partitionId(partitionId)
        .build();
  }
}
