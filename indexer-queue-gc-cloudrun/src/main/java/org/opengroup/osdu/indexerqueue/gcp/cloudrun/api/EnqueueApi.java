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

package org.opengroup.osdu.indexerqueue.gcp.cloudrun.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.CloudTaskRequest;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.core.common.model.search.SearchServiceRole;
import org.opengroup.osdu.core.gcp.util.HeadersInfo;
import org.opengroup.osdu.indexerqueue.gcp.cloudrun.config.IndexerQueueConfigProperties;
import org.opengroup.osdu.indexerqueue.gcp.cloudrun.oqm.publish.MessagePublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/_ah/push-handlers")
public class EnqueueApi {

	private final ObjectWriter writer = new ObjectMapper().writerFor(RecordChangedMessages.class);
	private final HeadersInfo headersInfo;
	private final MessagePublisher publisher;
	private final RecordChangedMessages message;
	private final IndexerQueueConfigProperties properties;

	@PostMapping(value = "/enqueue",produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@authorizationFilter.pubSubTaskHasRole('" + SearchServiceRole.ADMIN + "')")
	public ResponseEntity<String> enqueueTask() throws IOException {
		putAdditionalHeaders();

		CloudTaskRequest cloudTaskRequest = CloudTaskRequest.builder()
				.message(writer.writeValueAsString(message))
				.url(properties.getDefaultRelativeIndexerWorkerUrl())
				.build();

		HttpStatus response = publisher.sendMessage(cloudTaskRequest, headersInfo.getHeaders());
		return new ResponseEntity<>("", response);
	}

	private void putAdditionalHeaders() {
		DpsHeaders headers = this.headersInfo.getHeaders();
		headers.getHeaders().put(DpsHeaders.ACCOUNT_ID, message.getDataPartitionId());
		headers.put(DpsHeaders.DATA_PARTITION_ID, message.getDataPartitionId());
		if (message.hasCorrelationId()) {
			headers.put(DpsHeaders.CORRELATION_ID, message.getCorrelationId());
		}
	}
}
