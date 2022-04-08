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

import com.google.cloud.tasks.v2.CloudTasksClient;
import com.google.cloud.tasks.v2.HttpRequest;
import com.google.cloud.tasks.v2.QueueName;
import com.google.cloud.tasks.v2.Task;
import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.CloudTaskRequest;
import org.opengroup.osdu.indexerqueue.gcp.cloudrun.config.IndexerQueueConfigProperties;
import org.opengroup.osdu.indexerqueue.gcp.cloudrun.util.CloudTaskIdResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Case when `indexer-queue.task.enable = true` && `oqmDriver != pubsub` is invalid.
 * Missing bean exception says about invalid env configuration.
 */
@ConditionalOnExpression("'${index-task-type}'=='cloud-task' and '${oqmDriver}'=='pubsub'")
@Slf4j
@RequiredArgsConstructor
@Component
public class CloudTaskMessagePublisher implements MessagePublisher {

  private final CloudTaskIdResolver cloudTaskIdResolver;
  private final IndexerQueueConfigProperties config;
  private DpsHeaders headers;

  /**
   * @param request can be two types, normal indexing process with storage records ids
   * <pre>
   * {
   *     "data": "[{\"id\":\"opendes:work-product-component--TestSchema:123\",\"kind\":\"opendes:wks:work-product-component--TestSchema:1.0.0\",\"op\":\"create\"}]",
   *     "attributes": {
   *         "correlation-id": "74c20433-544f-46e3-a215-c059b2ca6810",
   *         "data-partition-id": "opendes"
   *     }
   * }
   * </pre>
   * Or reprocessing task, with kind and cursor
   * <pre>
   * {
   *    "kind":"opendes:wks:work-product-component--TestSchema:1.0.0",
   *    "cursor":"abc..."
   * }
   * </pre>
   * Depending on its type these tasks should be processed by different indexer endpoints,
   * for tasks with storage records ids usually used indexer endpoint /_dps/task-handlers/index-worker may not be included in the request, and then the default value will be used.
   * For tasks with kind and cursor usually relative URL present in the request, and it must be used instead of default endpoint.
   * @return http status.
   * @throws IOException ex.
   */
    @Override
    public HttpStatus sendMessage(CloudTaskRequest request, DpsHeaders headers) throws IOException {
      this.headers = headers;
      String partitionId = Optional.of(headers.getPartitionId()).orElseThrow(
          () -> new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
              "Partition id isn't set",
              String.format("Partition id isn't set for message: %s", request.getMessage())));
      log.info(String.format(
          "project-id: %s | location: %s | queue-id: %s | indexer-host: %s | message: %s",
          config.getGoogleCloudProject(), config.getGoogleCloudProjectRegion(),
          cloudTaskIdResolver.getQueueId(partitionId),
          config.getCloudTaskTargetHost() + request.getUrl(),
          request.getMessage()));

      String queuePath = QueueName
          .of(config.getGoogleCloudProject(), config.getGoogleCloudProjectRegion(),
              cloudTaskIdResolver.getQueueId(partitionId))
          .toString();

      return createTask(queuePath, request);
    }

  @NotNull
  private HttpStatus createTask(String queuePath, CloudTaskRequest request) throws IOException {
    try (CloudTasksClient client = CloudTasksClient.create()) {
      Task response = client.createTask(queuePath, getTask(request));
      log.info(String.format("task created: %s", response.getName()));
      return HttpStatus.OK;
    }
  }

  @NotNull
  private Task getTask(CloudTaskRequest request) {
    return Task.newBuilder()
        .setScheduleTime(Timestamp.newBuilder()
            .setSeconds(Instant.now(Clock.systemUTC()).getEpochSecond())
            .build())
        .setHttpRequest(getHttpRequest(request))
        .build();
  }

  @NotNull
  private HttpRequest getHttpRequest(CloudTaskRequest request) {
    return HttpRequest.newBuilder()
        .setUrl(config.getCloudTaskTargetHost() + request.getUrl())
        .setBody(ByteString.copyFrom(request.getMessage(), Charset.defaultCharset()))
        .putAllHeaders(headers != null ? headers.getHeaders() : Collections.emptyMap())
        .build();
  }
}
