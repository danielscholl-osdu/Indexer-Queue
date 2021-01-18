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

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.Constants;
import org.opengroup.osdu.core.common.http.HttpClient;
import org.opengroup.osdu.core.common.http.HttpRequest;
import org.opengroup.osdu.core.common.http.HttpResponse;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.indexerqueue.reference.config.IndexerQueueConfigProperties;
import org.opengroup.osdu.indexerqueue.reference.messagebus.IMessageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Subcriber {

  private final Gson gson = new Gson();

  private static final Logger logger = LoggerFactory.getLogger(Subcriber.class);

  private final DpsHeaders dpsHeaders = new DpsHeaders();
  private final IndexerQueueConfigProperties indexerQueueConfigProperties;

  @Autowired
  public Subcriber(IndexerQueueConfigProperties indexerQueueConfigProperties) {
    this.indexerQueueConfigProperties = indexerQueueConfigProperties;
  }

  @RabbitListener(queues = IMessageFactory.DEFAULT_QUEUE_NAME)
  public void recievedMessage(Message message) {
    byte[] body = message.getBody();
    String msg = new String(body);

    logger.info("Recieved Message: " + msg);
    JsonObject jsonContent = new JsonParser().parse(msg)
        .getAsJsonObject();
    String authorization = jsonContent.get("authorization").getAsString();

    RecordChangedMessages recordMessage = getTaskQueueMessage(msg);
    Map<String, String> attributes = recordMessage.getAttributes();
    attributes.put("authorization", authorization);

    logger.info(String.format("recordMessage: %s", recordMessage.toString()));

    dpsHeaders.getHeaders().put(DpsHeaders.ACCOUNT_ID, recordMessage.getDataPartitionId());
    dpsHeaders.getHeaders()
        .put(DpsHeaders.DATA_PARTITION_ID, recordMessage.getDataPartitionId());
    if (recordMessage.hasCorrelationId()) {
      dpsHeaders.getHeaders().put(DpsHeaders.CORRELATION_ID, recordMessage.getCorrelationId());
    }

    logger.info(String.format("message headers: %s", dpsHeaders.toString()));
    logger.info(String.format("message body: %s", gson.toJson(recordMessage)));

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

  private RecordChangedMessages getTaskQueueMessage(String msg) {
    try {
      JsonParser jsonParser = new JsonParser();
      JsonElement jsonMessage = jsonParser.parse(msg);

      RecordChangedMessages recordChangedMessages = gson
          .fromJson(jsonMessage.toString(), RecordChangedMessages.class);
      String payload = recordChangedMessages.getData();
      if (Strings.isNullOrEmpty(payload)) {
        logger.error("message data not found");
        throw new AppException(HttpStatus.SC_BAD_REQUEST, "Invalid record change message",
            "message data not found", "'message.data' not found in PubSub message");
      }

      Map<String, String> attributes = recordChangedMessages.getAttributes();
      if (attributes == null || attributes.size() == 0) {
        attributes = new HashMap<>();
        JsonObject jsonObjectMessage = jsonMessage.getAsJsonObject();
        attributes.put(DpsHeaders.DATA_PARTITION_ID,
            jsonObjectMessage.get(DpsHeaders.DATA_PARTITION_ID).getAsString());
        attributes.put(DpsHeaders.CORRELATION_ID,
            jsonObjectMessage.get(DpsHeaders.CORRELATION_ID).getAsString());
        recordChangedMessages.setAttributes(attributes);
      }
      Map<String, String> lowerCase = new HashMap<>();
      attributes.forEach((key, value) -> lowerCase.put(key.toLowerCase(), value));
      recordChangedMessages.setAttributes(lowerCase);
      if (recordChangedMessages.missingAccountId()) {
        logger.warn("tenant-id missing");
        throw new AppException(HttpStatus.SC_BAD_REQUEST, "Invalid tenant", "tenant-id missing",
            String.format("PubSub message: %s", recordChangedMessages));
      }

      return recordChangedMessages;
    } catch (JsonParseException e) {
      logger.warn("Unable to parse request payload.", e);
      throw new AppException(HttpStatus.SC_BAD_REQUEST, "Request payload parsing error",
          "Unable to parse request payload.", e);
    }
  }
}
