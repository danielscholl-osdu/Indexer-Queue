// Copyright © Microsoft Corporation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.indexerqueue.azure.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.azure.servicebus.IMessage;
import lombok.extern.java.Log;
import org.springframework.stereotype.Component;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.ofNullable;
import static org.opengroup.osdu.core.common.model.http.DpsHeaders.CORRELATION_ID;
import static org.opengroup.osdu.core.common.model.http.DpsHeaders.DATA_PARTITION_ID;


@Component
@Log
public class ServiceBusMessageAttributesExtractor implements MessageAttributesExtractor {

    private static final String MESSAGE_NODE = "message";

    public RecordChangedAttributes extracRecordChangedtAttributesFromMessageBody(IMessage message) {
      String messageBody = new String(message.getMessageBody().getBinaryData().get(0), UTF_8);
      JsonElement messageBodyJsonElement =  JsonParser.parseString(messageBody);
      return ofNullable(getElementAsJsonObject(messageBodyJsonElement))
        .map(jsonObject -> getElementAsJsonObject(jsonObject.get(MESSAGE_NODE)))
        .map(jsonObject ->
            RecordChangedAttributes.builder()
              .correlationId(jsonObject.get(CORRELATION_ID).getAsString())
              .dataPartitionId(jsonObject.get(DATA_PARTITION_ID).getAsString())
              .build()
        ).orElseGet(() -> {
            log.warning("Unable to parse body of message " + messageBody);
            return new RecordChangedAttributes();
        });
    }

    @Override
    public SchemaChangedAttributes extractSchemaChangedAttributesFromMessageBody(IMessage message) {
        String messageBody = new String(message.getMessageBody().getBinaryData().get(0), UTF_8);
        JsonElement messageBodyJsonElement =  JsonParser.parseString(messageBody);
        return ofNullable(getElementAsJsonObject(messageBodyJsonElement))
            .map(jsonObject -> getElementAsJsonObject(jsonObject.get(MESSAGE_NODE)))
            .map(jsonObject ->
                SchemaChangedAttributes.builder()
                    .correlationId(jsonObject.get(CORRELATION_ID).getAsString())
                    .dataPartitionId(jsonObject.get(DATA_PARTITION_ID).getAsString())
                    .build()
            ).orElseGet(() -> {
                log.warning("Unable to parse body of message " + messageBody);
                return new SchemaChangedAttributes();
            });
    }

    private JsonObject getElementAsJsonObject(JsonElement jsonElement) {
        if(jsonElement != null && !jsonElement.isJsonNull() && jsonElement.isJsonObject()) {
            return jsonElement.getAsJsonObject();
        }
        return null;
    }

}
