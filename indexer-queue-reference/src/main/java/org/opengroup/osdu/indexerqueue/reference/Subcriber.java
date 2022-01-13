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

package org.opengroup.osdu.indexerqueue.reference;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.opengroup.osdu.core.common.Constants;
import org.opengroup.osdu.core.common.http.HttpClient;
import org.opengroup.osdu.core.common.http.HttpRequest;
import org.opengroup.osdu.core.common.http.HttpResponse;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.indexerqueue.reference.auth.GcpServiceAccountJwtClient;
import org.opengroup.osdu.indexerqueue.reference.config.IndexerQueueConfigProperties;
import org.opengroup.osdu.indexerqueue.reference.messagebus.IMessageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Subcriber {

    private static final Logger logger = LoggerFactory.getLogger(Subcriber.class);

    private final IndexerQueueConfigProperties indexerQueueConfigProperties;

    private final GcpServiceAccountJwtClient jwtClient;

    @RabbitListener(queues = IMessageFactory.DEFAULT_QUEUE_NAME)
    public void recievedMessage(Message message) {
        byte[] body = message.getBody();

        Map<String, Object> headers = message.getMessageProperties().getHeaders();

        Map<String, String> headersMap = headers.entrySet().stream()
            .filter(header -> header.getValue() instanceof String)
            .collect(Collectors.toMap(Entry::getKey, e -> (String) e.getValue()));

        String token = jwtClient.getDefaultOrInjectedServiceAccountIdToken();
        headersMap.put(DpsHeaders.AUTHORIZATION, "Bearer " + token);

        String msg = new String(body);

        logger.info("Received Message: " + msg);
        logger.info("Received Headers: " + headers);

        RecordChangedMessages recordMessage = new RecordChangedMessages();
        recordMessage.setData(msg);
        recordMessage.setAttributes(headersMap);

        DpsHeaders dpsHeaders = DpsHeaders.createFromMap(headersMap);

        String url = StringUtils
            .join(indexerQueueConfigProperties.getIndexerUrl(), Constants.WORKER_RELATIVE_URL);
        HttpClient httpClient = new HttpClient();
        HttpRequest rq = HttpRequest.post(recordMessage).url(url).headers(dpsHeaders.getHeaders())
            .build();
        HttpResponse result = httpClient.send(rq);
        if (result.hasException()) {
            logger.error(result.getException().getLocalizedMessage(), result.getException());
        }
    }
}
