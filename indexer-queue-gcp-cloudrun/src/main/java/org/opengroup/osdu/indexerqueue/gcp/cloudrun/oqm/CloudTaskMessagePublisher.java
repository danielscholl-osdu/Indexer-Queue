package org.opengroup.osdu.indexerqueue.gcp.cloudrun.oqm;

import com.google.cloud.tasks.v2.CloudTasksClient;
import com.google.cloud.tasks.v2.HttpRequest;
import com.google.cloud.tasks.v2.QueueName;
import com.google.cloud.tasks.v2.Task;
import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.gcp.util.HeadersInfo;
import org.opengroup.osdu.indexerqueue.gcp.cloudrun.config.PropertiesConfiguration;
import org.opengroup.osdu.indexerqueue.gcp.cloudrun.util.IndexerQueueIdentifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Clock;
import java.time.Instant;

@ConditionalOnExpression("'${indexer-queue.task.enable}' and '${oqmDriver}'=='pubsub'")
@Slf4j
@RequiredArgsConstructor
@Component
public class CloudTaskMessagePublisher implements MessagePublisher {

    final IndexerQueueIdentifier indexerQueueProvider;

    final HeadersInfo headersInfo;

    final PropertiesConfiguration config;

    @Override
    public HttpStatus sendMessage(String message) throws IOException {
        log.info(String.format("project-id: %s | location: %s | queue-id: %s | indexer-host: %s | message: %s",
                config.getGoogleCloudProject(), config.getGoogleCloudProjectRegion(), indexerQueueProvider.getQueueId(),
                config.getCloudTaskTargetHost(), message));

        String queuePath = QueueName
                .of(config.getGoogleCloudProject(), config.getGoogleCloudProjectRegion(), indexerQueueProvider.getQueueId())
                .toString();

        Task.Builder taskBuilder = getTaskBuilder(message);

        // Execute the request and return the created Task
        try (CloudTasksClient client = CloudTasksClient.create()) {
            Task response = client.createTask(queuePath, taskBuilder.build());
            log.info(String.format("task created: %s", response.getName()));
            return HttpStatus.OK;
        }
    }

    private HttpRequest getHttpRequest(String message) {
        return HttpRequest.newBuilder()
                .setUrl(config.getCloudTaskTargetHost())
                .setBody(ByteString.copyFrom(message, Charset.defaultCharset()))
                .putAllHeaders(this.headersInfo.getHeaders().getHeaders())
                .build();
    }

    private Task.Builder getTaskBuilder(String message) {
        return Task.newBuilder()
                .setScheduleTime(Timestamp.newBuilder()
                        .setSeconds(Instant.now(Clock.systemUTC()).getEpochSecond())
                        .build())
                .setHttpRequest(getHttpRequest(message));
    }
}
