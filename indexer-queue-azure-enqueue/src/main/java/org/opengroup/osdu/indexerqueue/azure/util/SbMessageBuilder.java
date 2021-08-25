package org.opengroup.osdu.indexerqueue.azure.util;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.opengroup.osdu.azure.logging.CoreLoggerFactory;
import org.opengroup.osdu.azure.logging.ICoreLogger;
import org.opengroup.osdu.core.common.Constants;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.indexerqueue.azure.config.ThreadDpsHeaders;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/***
 * A class to extract RecordChangedMessages from the serviceBusMessage. Extract properties like data-partition-id, correlation-id, account-id and set the thread context.
 */
@Component
public class SbMessageBuilder {

    @Autowired
    private ThreadDpsHeaders threadDpsHeaders;
    @Autowired
    private MdcContextMap mdcContextMap;

    public RecordChangedMessages getServiceBusMessage(String serviceBusMessage) throws IOException {

        final Gson gson = new Gson();
        JsonParser jsonParser = new JsonParser();
        JsonElement jsonRoot = jsonParser.parse(serviceBusMessage);
        JsonElement message;

        try {
            message = jsonRoot.getAsJsonObject().get("message");
        }
        catch (IllegalStateException e) {
            throw new AppException(org.apache.http.HttpStatus.SC_BAD_REQUEST,
                    "Invalid record change message",
                    "Could not fetch JSON object", e);
        }

        if (message == null) {
            throw new AppException(org.apache.http.HttpStatus.SC_BAD_REQUEST, "Invalid record change message",
                    "message object not found", "'message' object not found in Storage message");
        }

        // Data in service bus comes in as array converting it to string

        JsonElement data = message.getAsJsonObject().get(Constants.DATA);
        if (data == null || Strings.isNullOrEmpty(data.toString())) {
            throw new AppException(org.apache.http.HttpStatus.SC_BAD_REQUEST, "Invalid record change message",
                    "message data not found", "'message.data' not found in ServiceBus message");
        }
        String dataValue = data.toString();
        message.getAsJsonObject().addProperty(Constants.DATA, dataValue);

        Map<String, String> attributesMap = new HashMap<>();

        if (message.getAsJsonObject().get(DpsHeaders.ACCOUNT_ID) == null
                || message.getAsJsonObject().get(DpsHeaders.DATA_PARTITION_ID) == null) {
            throw new AppException(org.apache.http.HttpStatus.SC_BAD_REQUEST, "Invalid tenant", "tenant-id missing",
                    String.format("Service Bus message: %s", serviceBusMessage));
        }

        String correlationId;
        String dataPartitionId = message.getAsJsonObject().get(DpsHeaders.DATA_PARTITION_ID).getAsString();
        String accountId = message.getAsJsonObject().get(DpsHeaders.ACCOUNT_ID).getAsString();

        // Generate a correlation-id if it is null.
        if (message.getAsJsonObject().get(DpsHeaders.CORRELATION_ID) != null) {
            attributesMap.put(DpsHeaders.CORRELATION_ID,
                     message.getAsJsonObject().get(DpsHeaders.CORRELATION_ID).getAsString());
            correlationId = message.getAsJsonObject().get(DpsHeaders.CORRELATION_ID).getAsString();
        } else {
            correlationId = UUID.randomUUID().toString();
        }

        // Set the context for this thread.
        threadDpsHeaders.setThreadContext(dataPartitionId,correlationId,accountId);
        MDC.setContextMap(mdcContextMap.getContextMap(correlationId, dataPartitionId));

        // Populate attributes map for the recordChangedMessage.
        attributesMap.put(DpsHeaders.ACCOUNT_ID, accountId);
        attributesMap.put(DpsHeaders.DATA_PARTITION_ID, dataPartitionId);
        attributesMap.put(DpsHeaders.CORRELATION_ID, correlationId);

        RecordChangedMessages recordChangedMessage = gson.fromJson(message.toString(), RecordChangedMessages.class);
        recordChangedMessage.setAttributes(attributesMap);

        return recordChangedMessage;
    }
}
