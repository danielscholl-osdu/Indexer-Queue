// Copyright © Microsoft Corporation
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//      http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.indexerqueue.azure.util;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.Constants;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SBMessageBuilder {

  public RecordChangedMessages getServiceBusMessage(String serviceBusRequest) throws IOException {

    final Gson gson = new Gson();
    JsonParser jsonParser = new JsonParser();
    JsonElement jsonRoot = jsonParser.parse(serviceBusRequest);
    JsonElement message = jsonRoot.getAsJsonObject().get("message");
    if (message == null) {
      throw new AppException(org.apache.http.HttpStatus.SC_BAD_REQUEST, "Invalid record change message",
          "message object not found", "'message' object not found in Storage message");
    }

    // Data in service bus comes in as array converting it to string
    String dataValue = message.getAsJsonObject().get(Constants.DATA).toString();
    if (Strings.isNullOrEmpty(dataValue)) {
      throw new AppException(HttpStatus.SC_BAD_REQUEST, "Invalid record change message", "message data not found",
          "'message.data' not found in ServiceBus message");
    }
    message.getAsJsonObject().addProperty(Constants.DATA, dataValue);

    Map<String, String> attributesMap = new HashMap<>();

    if (message.getAsJsonObject().get(DpsHeaders.ACCOUNT_ID) == null
        || message.getAsJsonObject().get(DpsHeaders.DATA_PARTITION_ID) == null) {
      throw new AppException(org.apache.http.HttpStatus.SC_BAD_REQUEST, "Invalid tenant", "tenant-id missing",
          String.format("Service Bus message: %s", serviceBusRequest));
    }

    if (message.getAsJsonObject().get(DpsHeaders.ACCOUNT_ID) != null) {
      attributesMap.put(DpsHeaders.ACCOUNT_ID, message.getAsJsonObject().get(DpsHeaders.ACCOUNT_ID).getAsString());
    }
    if (message.getAsJsonObject().get(DpsHeaders.DATA_PARTITION_ID) != null) {
      attributesMap.put(DpsHeaders.DATA_PARTITION_ID,
          message.getAsJsonObject().get(DpsHeaders.DATA_PARTITION_ID).getAsString());
    }
    // TODO Create a correlation id if it is null
    if (message.getAsJsonObject().get(DpsHeaders.CORRELATION_ID) != null) {
      attributesMap.put(DpsHeaders.CORRELATION_ID,
          message.getAsJsonObject().get(DpsHeaders.CORRELATION_ID).getAsString());
    }

    RecordChangedMessages recordChangedMessages = gson.fromJson(message.toString(), RecordChangedMessages.class);
    recordChangedMessages.setAttributes(attributesMap);
    return recordChangedMessages;
  }
}