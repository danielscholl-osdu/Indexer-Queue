package org.opengroup.osdu.indexerqueue.gcp.cloudrun.oqm;

import com.google.cloud.tasks.v2.CloudTasksClient;
import com.google.cloud.tasks.v2.HttpRequest;
import com.google.cloud.tasks.v2.QueueName;
import com.google.cloud.tasks.v2.Task;
import com.google.common.base.Strings;
import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.common.model.search.CloudTaskRequest;
import org.opengroup.osdu.core.gcp.util.HeadersInfo;
import org.opengroup.osdu.indexerqueue.gcp.cloudrun.config.PropertiesConfiguration;
import org.opengroup.osdu.indexerqueue.gcp.cloudrun.util.IndexerQueueIdentifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@ConditionalOnExpression("'${indexer-queue.task.enable}' and '${oqmDriver}'=='pubsub'")
@Slf4j
@RequiredArgsConstructor
@Component
public class CloudTaskMessagePublisher implements MessagePublisher {

    final IndexerQueueIdentifier indexerQueueProvider;

    final HeadersInfo headersInfo;

    final PropertiesConfiguration config;

    /**
     * @param request can be two types, normal indexing process with storage records ids
     * {
     *     "data": "[{\"id\":\"opendes:work-product-component--TestSchema:123\",\"kind\":\"opendes:wks:work-product-component--TestSchema:1.0.0\",\"op\":\"create\"}]",
     *     "attributes": {
     *         "correlation-id": "74c20433-544f-46e3-a215-c059b2ca6810",
     *         "data-partition-id": "opendes"
     *     }
     * }
     * Or reprocessing task, with kind and cursor
     * {
     *    "kind":"opendes:wks:work-product-component--TestSchema:1.0.0",
     *    "cursor":"abc..."
     * }
     * Depending on its type these tasks should be processed by different indexer endpoints,
     * for tasks with storage records ids usually used indexer endpoint /_dps/task-handlers/index-worker may not be included in the request, and then the default value will be used.
     * For tasks with kind and cursor usually relative URL present in the request, and it must be used instead of default endpoint.
     * @return
     * @throws IOException
     */
    @Override
    public HttpStatus sendMessage(CloudTaskRequest request) throws IOException {
        if (Strings.isNullOrEmpty(request.getUrl())) {
            request.setUrl(config.getDefaultRelativeIndexerWorkerUrl());
        }
        log.info(String.format("project-id: %s | location: %s | queue-id: %s | indexer-host: %s | message: %s",
            config.getGoogleCloudProject(), config.getGoogleCloudProjectRegion(), indexerQueueProvider.getQueueId(),
            config.getCloudTaskTargetHost() + request.getUrl(), request.getMessage()));

        String queuePath = QueueName
            .of(config.getGoogleCloudProject(), config.getGoogleCloudProjectRegion(), indexerQueueProvider.getQueueId())
            .toString();

        Task.Builder taskBuilder = getTaskBuilder(request);

        // Execute the request and return the created Task
        try (CloudTasksClient client = CloudTasksClient.create()) {
            Task response = client.createTask(queuePath, taskBuilder.build());
            log.info(String.format("task created: %s", response.getName()));
            return HttpStatus.OK;
        }
    }

    private HttpRequest getHttpRequest(CloudTaskRequest request) {
        return HttpRequest.newBuilder()
            .setUrl(config.getCloudTaskTargetHost() + request.getUrl())
            .setBody(ByteString.copyFrom(request.getMessage(), Charset.defaultCharset()))
            .putAllHeaders(this.headersInfo.getHeaders().getHeaders())
            .build();
    }

    private Task.Builder getTaskBuilder(CloudTaskRequest request) {
        return Task.newBuilder()
            .setScheduleTime(Timestamp.newBuilder()
                .setSeconds(Instant.now(Clock.systemUTC()).getEpochSecond())
                .build())
            .setHttpRequest(getHttpRequest(request));
    }
}
