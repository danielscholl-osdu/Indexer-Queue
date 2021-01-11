// Copyright 2017-2019, Schlumberger
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.indexerqueue.gcp.util;

import com.google.cloud.tasks.v2.*;
import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import lombok.extern.java.Log;
import org.opengroup.osdu.core.common.model.search.CloudTaskRequest;
import org.opengroup.osdu.core.gcp.util.HeadersInfo;
import org.opengroup.osdu.indexerqueue.gcp.config.IndexerQueueConfigurationPropertiesGcp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Clock;
import java.time.Instant;

@Log
@Component
public class AppEngineTaskBuilder {

    @Autowired
    private IndexerQueueConfigurationPropertiesGcp configurationProperties;
    @Autowired
    private IndexerQueueIdentifier indexerQueueProvider;
    @Autowired
    private HeadersInfo headersInfo;

    public Task createTask(CloudTaskRequest request) throws IOException {
        this.log.info(String.format("project-id: %s | location: %s | queue-id: %s | indexer-host: %s | message: %s",
                configurationProperties.getGoogleCloudProject(),
                configurationProperties.getDeploymentLocation(),
                indexerQueueProvider.getQueueId(),
                configurationProperties.getIndexerHost(),
                request.getMessage()));
        String queuePath = QueueName.of(
                configurationProperties.getGoogleCloudProject(),
                configurationProperties.getDeploymentLocation(),
                indexerQueueProvider.getQueueId()).toString();
        AppEngineRouting routing = AppEngineRouting.newBuilder()
                .setHost(configurationProperties.getIndexerHost())
                .build();
        Task.Builder taskBuilder = Task
                .newBuilder()
                .setScheduleTime(Timestamp.newBuilder()
                        .setSeconds(Instant.now(Clock.systemUTC()).plusMillis(request.getInitialDelayMillis()).getEpochSecond())
                        .build())
                .setAppEngineHttpRequest(AppEngineHttpRequest.newBuilder()
                        .putAllHeaders(this.headersInfo.getHeaders().getHeaders())
                        .setBody(ByteString.copyFrom(request.getMessage(), Charset.defaultCharset()))
                        .setRelativeUri(request.getUrl())
                        .setAppEngineRouting(routing)
                        .setHttpMethod(HttpMethod.POST)
                        .build());

        // Execute the request and return the created Task
        try (CloudTasksClient client = CloudTasksClient.create()) {
            Task response = client.createTask(queuePath, taskBuilder.build());
            this.log.info(String.format("task created: %s", response.getName()));
            return response;
        }
    }
}