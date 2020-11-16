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
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.common.Constants;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.CloudTaskRequest;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.core.common.model.search.SearchServiceRole;
import org.opengroup.osdu.core.gcp.util.HeadersInfo;
import org.opengroup.osdu.indexerqueue.gcp.cloudrun.util.TaskBuilder;
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

	final HttpServletRequest request;

	final HeadersInfo headersInfo;

	final TaskBuilder taskBuilder;

	final RecordChangedMessages message;

	@PostMapping("/enqueue")
	@PreAuthorize("@authorizationFilter.pubSubTaskHasRole('" + SearchServiceRole.ADMIN + "')")
	public ResponseEntity enqueueTask() throws IOException {
		this.headersInfo.getHeaders().getHeaders().put(DpsHeaders.ACCOUNT_ID, message.getDataPartitionId());
		this.headersInfo.getHeaders().put(DpsHeaders.DATA_PARTITION_ID, message.getDataPartitionId());
		if (message.hasCorrelationId()) {
			this.headersInfo.getHeaders().put(DpsHeaders.CORRELATION_ID, message.getCorrelationId());
		}

		CloudTaskRequest cloudTaskRequest = CloudTaskRequest.builder()
			.message(writer.writeValueAsString(message))
			.url(Constants.WORKER_RELATIVE_URL).build();
		this.taskBuilder.createTask(cloudTaskRequest);

		return new ResponseEntity(org.springframework.http.HttpStatus.OK);
	}


}
