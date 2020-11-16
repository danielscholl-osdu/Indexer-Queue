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

import java.io.IOException;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.common.SwaggerDoc;
import org.opengroup.osdu.core.common.model.search.CloudTaskRequest;
import org.opengroup.osdu.core.common.model.search.SearchServiceRole;
import org.opengroup.osdu.indexerqueue.gcp.cloudrun.util.TaskBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/_dps/task-handlers")
public class TaskApi {

	final TaskBuilder taskBuilder;

	@PostMapping("/enqueue")
	@PreAuthorize("@authorizationFilter.hasRole('" + SearchServiceRole.ADMIN + "')")
	public ResponseEntity enqueueTask(
		@NotNull(message = SwaggerDoc.REQUEST_VALIDATION_NOT_NULL_BODY) @Valid @RequestBody CloudTaskRequest request)
		throws IOException {

		this.taskBuilder.createTask(request);

		return new ResponseEntity(HttpStatus.OK);
	}
}
