package org.opengroup.osdu.indexerqueue.azure.util;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.opengroup.osdu.core.common.Constants;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.indexer.SchemaChangedMessages;
import org.opengroup.osdu.indexerqueue.azure.config.ThreadDpsHeaders;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class SchemaChangedSbMessageBuilder {

    @Autowired
    private ThreadDpsHeaders threadDpsHeaders;
    @Autowired
    private MdcContextMap mdcContextMap;

    private Gson gson = new Gson();

    public SchemaChangedMessages buildSchemaChangedServiceBusMessage(String serviceBusMessage) {
        JsonParser jsonParser = new JsonParser();
        JsonElement jsonRoot = jsonParser.parse(serviceBusMessage);
        JsonElement message;

        try {
            message = jsonRoot.getAsJsonObject().get("message");
        }
        catch (IllegalStateException e) {
            throw new AppException(org.apache.http.HttpStatus.SC_BAD_REQUEST,
                "Invalid schema change message",
                "Could not fetch JSON object", e);
        }

        if (message == null) {
            throw new AppException(org.apache.http.HttpStatus.SC_BAD_REQUEST, "Invalid schema change message",
                "message object not found", "'message' object not found in Schema message");
        }

        // Data in service bus comes in as array converting it to string
        JsonElement data = message.getAsJsonObject().get(Constants.DATA);
        if (data == null || Strings.isNullOrEmpty(data.toString())) {
            throw new AppException(org.apache.http.HttpStatus.SC_BAD_REQUEST, "Invalid record change message",
                "message data not found", "'message.data' not found in ServiceBus message");
        }
        String dataValue = data.toString();
        message.getAsJsonObject().addProperty(Constants.DATA, dataValue);

        if (message.getAsJsonObject().get(DpsHeaders.ACCOUNT_ID) == null
            || message.getAsJsonObject().get(DpsHeaders.DATA_PARTITION_ID) == null) {
            throw new AppException(org.apache.http.HttpStatus.SC_BAD_REQUEST, "Invalid tenant", "tenant-id missing",
                String.format("Service Bus message: %s", serviceBusMessage));
        }

        String dataPartitionId = message.getAsJsonObject().get(DpsHeaders.DATA_PARTITION_ID).getAsString();
        String correlationId = message.getAsJsonObject().get(DpsHeaders.CORRELATION_ID).getAsString();

        threadDpsHeaders.setThreadContext(dataPartitionId,correlationId);
        MDC.setContextMap(mdcContextMap.getContextMap(correlationId, dataPartitionId));

        // Populate attributes map for the recordChangedMessage.
        Map<String, String> attributesMap = new HashMap<>();
        attributesMap.put(DpsHeaders.DATA_PARTITION_ID, dataPartitionId);
        attributesMap.put(DpsHeaders.CORRELATION_ID, correlationId);

        SchemaChangedMessages schemaChangedMessages = gson.fromJson(message.toString(), SchemaChangedMessages.class);
        schemaChangedMessages.setAttributes(attributesMap);

        return schemaChangedMessages;
    }
}
