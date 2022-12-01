/*
 * Copyright 2020-2022 Google LLC
 * Copyright 2020-2022 EPAM Systems, Inc
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

import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.CloudTaskRequest;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.indexerqueue.gcp.cloudrun.config.IndexerQueueConfigProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * RestMessagePublisher supports PUSH approach, sends incoming OqmMessages with indexing tasks to Indexer REST API.
 */
@ConditionalOnExpression("'${index-task-type}'=='rest'")
@RequiredArgsConstructor
@Component
@Slf4j
public class RestPublisher implements MessagePublisher {

    private static final String QUEUENAME = "x-cloudtasks-queuename";
    private final IndexerQueueConfigProperties properties;
    private final AsyncProxyUtil asyncProxyUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public HttpStatus sendMessage(CloudTaskRequest request, DpsHeaders dpsHeaders) {
        Object body;
        if (request.getUrl().equalsIgnoreCase(properties.getDefaultRelativeIndexerWorkerUrl())) {
            body = getIndexWorkerRequestBody(request, dpsHeaders);
        } else {
            body = request.getMessage();
        }
        log.info("RestMessagePublisher sending request: {}", request);
        asyncProxyUtil.asyncTask(body, getHttpHeaders(dpsHeaders), HttpMethod.POST, getUri(properties.getIndexerHost() + request.getUrl()));
        return HttpStatus.OK;
    }

    private RecordChangedMessages getIndexWorkerRequestBody(CloudTaskRequest request, DpsHeaders dpsHeaders) {
        RecordChangedMessages messages;
        try {
            messages = objectMapper.readValue(request.getMessage(), RecordChangedMessages.class);
        } catch (JsonProcessingException ignored) {
            messages = RecordChangedMessages.builder()
                .messageId(dpsHeaders.getCorrelationId())
                .data(request.getMessage())
                .attributes(getHttpHeaders(dpsHeaders).toSingleValueMap())
                .publishTime(LocalDateTime.now().toString())
                .build();
        }
        return messages;
    }

    @NotNull
    private HttpHeaders getHttpHeaders(DpsHeaders headers) {
        HttpHeaders headersMap = new HttpHeaders();
        headersMap.set(DpsHeaders.ACCOUNT_ID, headers.getAccountId());
        headersMap.set(DpsHeaders.DATA_PARTITION_ID, headers.getPartitionId());
        headersMap.set(DpsHeaders.CORRELATION_ID, headers.getCorrelationId());
        headersMap.set(QUEUENAME, headers.getPartitionId());
        headersMap.setContentType(MediaType.APPLICATION_JSON);
        return headersMap;
    }

    private URI getUri(String url) {
        try {
            return new URI(url);
        } catch (URISyntaxException e) {
            throw new AppException(SC_INTERNAL_SERVER_ERROR, e.getReason(), e.getMessage());
        }
    }
}
