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

package org.opengroup.osdu.indexerqueue.gcp.cloudrun.middleware;

import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.common.model.entitlements.AuthorizationResponse;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.core.common.provider.interfaces.IAuthorizationService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Slf4j
@RequiredArgsConstructor
@Component("authorizationFilter")
@RequestScope
public class AuthorizationFilter {

	final IAuthorizationService authorizationService;

	final DpsHeaders headers;

	final HttpServletRequest request;

	final RecordChangedMessages message;

	public boolean pubSubTaskHasRole(String... requiredRoles) {
		headers.getHeaders().put(DpsHeaders.ACCOUNT_ID, message.getDataPartitionId());
		headers.getHeaders().put(DpsHeaders.DATA_PARTITION_ID, message.getDataPartitionId());
		if (message.hasCorrelationId()) {
			headers.getHeaders().put(DpsHeaders.CORRELATION_ID, message.getCorrelationId());
		}
		AuthorizationResponse authResponse = authorizationService.authorizeAny(headers, requiredRoles);
		headers.put(DpsHeaders.USER_EMAIL, authResponse.getUser());
		return true;
	}

	public boolean hasRole(String... requiredRoles) {
		AuthorizationResponse authResponse = authorizationService.authorizeAny(headers, requiredRoles);
		headers.put(DpsHeaders.USER_EMAIL, authResponse.getUser());
		return true;
	}

}
