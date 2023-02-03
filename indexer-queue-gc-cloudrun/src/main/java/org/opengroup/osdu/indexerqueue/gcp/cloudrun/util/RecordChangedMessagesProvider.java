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
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Slf4j
@Component
public class RecordChangedMessagesProvider {

  private static final String INVALID_RECORD_CHANGE_MESSAGE = "Invalid record change message";

  private final Gson gson = new Gson();
  private RecordChangedMessages recordChangedMessages;

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.TARGET_CLASS)
  public RecordChangedMessages getTaskQueueMessage(HttpServletRequest request) {
    String requestBody = getRequestBody(request);
    log.info("Request body : " + requestBody);
    JsonElement message = getMessage(requestBody);
    checkMessage(message);

    recordChangedMessages = this.gson.fromJson(message.toString(), RecordChangedMessages.class);

    decodeData();
    formatAttributes();
    checkAccountId();
    return recordChangedMessages;
  }

  @NotNull
  private String getRequestBody(HttpServletRequest request) {
    try {
      return request.getReader().lines().collect(Collectors.joining("\n"));
    } catch (IOException e) {
      throw new AppException(HttpStatus.SC_BAD_REQUEST, "Request payload parsing error",
          "Unable to parse request payload.", e);
    }
  }

  private JsonElement getMessage(String requestBody) {
    return JsonParser.parseString(requestBody).getAsJsonObject().get("message");
  }

  private void checkMessage(JsonElement message) {
    if (message == null) {
      throw new AppException(HttpStatus.SC_BAD_REQUEST, INVALID_RECORD_CHANGE_MESSAGE,
          "message object not found", "'message' object not found in message");
    }
  }

  private void decodeData() {
    String payload = recordChangedMessages.getData();
    checkPayload(payload);
    String decodedPayload = new String(Base64.getDecoder().decode(payload));
    recordChangedMessages.setData(decodedPayload);
  }

  private void checkPayload(String payload) {
    if (Strings.isNullOrEmpty(payload)) {
      throw new AppException(HttpStatus.SC_BAD_REQUEST, INVALID_RECORD_CHANGE_MESSAGE,
          "message data not found", "'message.data' not found in PubSub message");
    }
  }

  private void formatAttributes() {
    Map<String, String> attributes = recordChangedMessages.getAttributes();
    checkAttributes(attributes);
    Map<String, String> lowerCase = new HashMap<>();
    attributes.forEach((key, value) -> lowerCase.put(key.toLowerCase(), value));
    recordChangedMessages.setAttributes(lowerCase);
  }

  private void checkAttributes(Map<String, String> attributes) {
    if (attributes == null || attributes.size() == 0) {
      throw new AppException(HttpStatus.SC_BAD_REQUEST, INVALID_RECORD_CHANGE_MESSAGE,
          "attribute map not found", String.format("PubSub message: %s", recordChangedMessages));
    }
  }

  private void checkAccountId() {
    if (recordChangedMessages.missingAccountId()) {
      throw new AppException(HttpStatus.SC_BAD_REQUEST, "Invalid tenant", "tenant-id missing",
          String.format("PubSub message: %s", recordChangedMessages));
    }
  }
}
