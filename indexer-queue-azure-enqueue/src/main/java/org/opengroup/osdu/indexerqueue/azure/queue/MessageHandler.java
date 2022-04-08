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

package org.opengroup.osdu.indexerqueue.azure.queue;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.SubscriptionClient;
import org.opengroup.osdu.azure.logging.CoreLoggerFactory;
import org.opengroup.osdu.azure.logging.ICoreLogger;
import org.opengroup.osdu.azure.servicebus.AbstractMessageHandler;
import org.opengroup.osdu.core.common.model.indexer.RecordInfo;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.indexerqueue.azure.metrics.IMetricService;
import org.opengroup.osdu.indexerqueue.azure.scope.thread.ThreadScopeContextHolder;
import org.opengroup.osdu.indexerqueue.azure.util.SbMessageBuilder;
import org.slf4j.MDC;

import java.lang.reflect.Type;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

/***
 * Defines callback for receiving messages from Azure Service Bus.
 */
public class MessageHandler extends AbstractMessageHandler {

    private RecordChangedMessageHandler recordChangedMessageHandler;
    private SbMessageBuilder sbMessageBuilder;
    private ICoreLogger Logger = CoreLoggerFactory.getInstance().getLogger(MessageHandler.class.getName());
    private IMetricService metricService;

    MessageHandler(SubscriptionClient client, RecordChangedMessageHandler recordChangedMessageHandler, SbMessageBuilder sbMessageBuilder, IMetricService metricService, String appName) {
        super(appName, client);
        this.recordChangedMessageHandler = recordChangedMessageHandler;
        this.sbMessageBuilder = sbMessageBuilder;
        this.metricService = metricService;
    }

    /*
     * Receives a single batch of messages from service bus and sends to builds it as `RecordChangedMessages` for indexer service.
     * One batch of messages from storage service to service bus has upto 50 `PubSubInfo` messages.
     */
    public void processMessage(IMessage message) throws Exception {
        String messageBody = new String(message.getMessageBody().getBinaryData().get(0), UTF_8);
        long enqueueTime = message.getEnqueuedTimeUtc().toEpochMilli();
        String messageId = message.getMessageId();

        try {
            RecordChangedMessages recordChangedMessage = sbMessageBuilder.getServiceBusMessage(messageBody, messageId);
            recordChangedMessage.setPublishTime(message.getEnqueuedTimeUtc().toString());
            recordChangedMessage.setMessageId(messageId);

            recordChangedMessageHandler.sendMessagesToIndexer(recordChangedMessage, message.getDeliveryCount());

            long stopTime = System.currentTimeMillis();
            this.captureMetrics(recordChangedMessage, enqueueTime, stopTime);

            ThreadScopeContextHolder.getContext().clear();
            MDC.clear();

        } catch (Exception e) {
            ThreadScopeContextHolder.getContext().clear();
            MDC.clear();

            throw e;
        }
    }

    private void captureMetrics(RecordChangedMessages recordChangedMessage, long enqueueTime, long stopTime) {
        try {
            Type listType = new TypeToken<List<RecordInfo>>() {}.getType();
            List<RecordInfo> recordInfos = new Gson().fromJson(recordChangedMessage.getData(), listType);
            for(RecordInfo record: recordInfos) {
                this.metricService.sendIndexLatencyMetric(stopTime - enqueueTime);
            }
        } catch (Exception e) {
            Logger.error("Error recording metrics", e);
        }
    }
}
