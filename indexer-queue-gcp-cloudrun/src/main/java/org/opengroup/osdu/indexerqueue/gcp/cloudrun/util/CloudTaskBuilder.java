/*
 * Copyright 2020 Google LLC
 * Copyright 2020 EPAM Systems, Inc
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

package org.opengroup.osdu.indexerqueue.gcp.cloudrun.util;

import com.google.cloud.tasks.v2.QueueName;
import com.google.cloud.tasks.v2beta3.CloudTasksClient;
import com.google.cloud.tasks.v2beta3.HttpRequest;
import com.google.cloud.tasks.v2beta3.OidcToken;
import com.google.cloud.tasks.v2beta3.Task;
import com.google.cloud.tasks.v2beta3.Task.Builder;
import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.common.model.search.CloudTaskRequest;
import org.opengroup.osdu.core.common.search.Config;
import org.opengroup.osdu.core.gcp.util.HeadersInfo;
import org.opengroup.osdu.indexerqueue.gcp.cloudrun.config.PropertiesConfiguration;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class CloudTaskBuilder implements TaskBuilder {

	final IndexerQueueIdentifier indexerQueueProvider;

	final HeadersInfo headersInfo;

	final PropertiesConfiguration configuration;

	public Task createTask(CloudTaskRequest request) throws IOException {
		log.info(String.format("project-id: %s | location: %s | queue-id: %s | indexer-host: %s | message: %s",
			Config.getGoogleCloudProjectId(), Config.getDeploymentLocation(), indexerQueueProvider.getQueueId(),
			Config.getIndexerHostUrl(), request.getMessage()));

		String queuePath = QueueName
			.of(Config.getGoogleCloudProjectId(), Config.getDeploymentLocation(), indexerQueueProvider.getQueueId())
			.toString();

		OidcToken.Builder oidcTokenBuilder = OidcToken.newBuilder()
			.setServiceAccountEmail(configuration.getServiceMail()).setAudience(configuration.getGoogleAudience());

		HttpRequest httpRequest = HttpRequest.newBuilder()
			.setUrl(Config.getIndexerHostUrl())
			.setBody(ByteString.copyFrom(request.getMessage(), Charset.defaultCharset()))
			.setOidcToken(oidcTokenBuilder)
			.putAllHeaders(this.headersInfo.getHeaders().getHeaders())
			.build();

		Builder taskBuilder = Task.newBuilder()
			.setScheduleTime(Timestamp.newBuilder()
				.setSeconds(Instant.now(Clock.systemUTC()).plusMillis(request.getInitialDelayMillis()).getEpochSecond())
				.build())
			.setHttpRequest(httpRequest);

		// Execute the request and return the created Task
		try (CloudTasksClient client = CloudTasksClient.create()) {
			Task response = client.createTask(queuePath, taskBuilder.build());
			log.info(String.format("task created: %s", response.getName()));
			return response;
		}
	}
}
