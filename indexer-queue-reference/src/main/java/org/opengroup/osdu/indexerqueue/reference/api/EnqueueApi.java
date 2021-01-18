/*
 * Copyright 2021 Google LLC
 * Copyright 2021 EPAM Systems, Inc
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

package org.opengroup.osdu.indexerqueue.reference.api;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.java.Log;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.indexerqueue.reference.util.TaskBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Log
@RestController
@RequestMapping("/_ah/push-handlers")
public class EnqueueApi {

  private final Gson gson = new Gson();
  @Autowired
  private HttpServletRequest request;
  @Autowired
  private DpsHeaders headersInfo;
  @Autowired
  private TaskBuilder taskBuilder;

  @PostMapping("/enqueue")
  public ResponseEntity enqueueTask() {
    RecordChangedMessages message = getTaskQueueMessage();
    headersInfo.getHeaders().put(DpsHeaders.ACCOUNT_ID, message.getDataPartitionId());
    headersInfo.getHeaders().put(DpsHeaders.DATA_PARTITION_ID, message.getDataPartitionId());
    if (message.hasCorrelationId()) {
      headersInfo.getHeaders().put(DpsHeaders.CORRELATION_ID, message.getCorrelationId());
    }
    log.info(String.format("message headers: %s", headersInfo.toString()));
    HttpStatus status = taskBuilder.createTask(message, headersInfo);

    return new ResponseEntity(status);
  }

  private RecordChangedMessages getTaskQueueMessage() {
    try {
      String errorText = "Invalid record change message";
      JsonParser jsonParser = new JsonParser();
      String requestBody = request.getReader().lines().collect(Collectors.joining("\n"));
      JsonElement jsonRoot = jsonParser.parse(requestBody);
      JsonElement message = jsonRoot.getAsJsonObject().get("message");
      if (message == null) {
        throw new AppException(HttpStatus.BAD_REQUEST.value(), errorText,
            "message object not found", "'message' object not found in PubSub message");
      }

      RecordChangedMessages recordChangedMessages = gson
          .fromJson(message.toString(), RecordChangedMessages.class);
      String payload = recordChangedMessages.getData();
      if (Strings.isNullOrEmpty(payload)) {
        throw new AppException(HttpStatus.BAD_REQUEST.value(), errorText, "message data not found",
            "'message.data' not found in PubSub message");
      }

      String decodedPayload = new String(Base64.getDecoder().decode(payload));
      recordChangedMessages.setData(decodedPayload);

      Map<String, String> attributes = recordChangedMessages.getAttributes();
      if (attributes == null || attributes.size() == 0) {
        throw new AppException(HttpStatus.BAD_REQUEST.value(), errorText, "attribute map not found",
            String.format("PubSub message: %s", recordChangedMessages));
      }
      Map<String, String> lowerCase = new HashMap<>();
      attributes.forEach((key, value) -> lowerCase.put(key.toLowerCase(), value));
      recordChangedMessages.setAttributes(lowerCase);
      if (recordChangedMessages.missingAccountId()) {
        throw new AppException(HttpStatus.BAD_REQUEST.value(), "Invalid tenant",
            "tenant-id missing", String.format("PubSub message: %s", recordChangedMessages));
      }
      return recordChangedMessages;
    } catch (IOException e) {
      throw new AppException(HttpStatus.BAD_REQUEST.value(), "Request payload parsing error",
          "Unable to parse request payload.", e);
    }
  }
}
