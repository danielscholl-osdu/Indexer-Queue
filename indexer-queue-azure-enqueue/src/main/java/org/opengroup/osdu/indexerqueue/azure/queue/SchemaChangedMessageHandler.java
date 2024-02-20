// Copyright © Schlumberger
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

package org.opengroup.osdu.indexerqueue.azure.queue;

import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.SubscriptionClient;
import org.opengroup.osdu.azure.servicebus.AbstractMessageHandler;
import org.opengroup.osdu.core.common.model.indexer.SchemaChangedMessages;
import org.opengroup.osdu.indexerqueue.azure.scope.thread.ThreadScopeContextHolder;
import org.opengroup.osdu.indexerqueue.azure.util.SchemaChangedSbMessageBuilder;
import org.slf4j.MDC;

import static java.nio.charset.StandardCharsets.UTF_8;

public class SchemaChangedMessageHandler extends AbstractMessageHandler {

    private SchemaChangedSbMessageBuilder schemaChangedSbMessageBuilder;
    private IndexUpdateMessageHandler indexUpdateMessageHandler;

    public SchemaChangedMessageHandler(String workerServiceName,
                                       SubscriptionClient client,
                                       SchemaChangedSbMessageBuilder schemaChangedSbMessageBuilder,
                                       IndexUpdateMessageHandler indexUpdateMessageHandler) {
        super(workerServiceName, client);
        this.schemaChangedSbMessageBuilder = schemaChangedSbMessageBuilder;
        this.indexUpdateMessageHandler = indexUpdateMessageHandler;
    }

    /*
     * Receives a single batch of messages from service bus and sends to builds it as `SchemaChangedMessages` for indexer service.
     * Messages are of type 'SchemaPubSubInfo'
     */
    public void processMessage(IMessage message) {
        String messageBody = new String(message.getMessageBody().getBinaryData().get(0), UTF_8);
        String messageId = message.getMessageId();

        try {
            SchemaChangedMessages schemaChangedMessages = schemaChangedSbMessageBuilder.buildSchemaChangedServiceBusMessage(messageBody);
            schemaChangedMessages.setPublishTime(message.getEnqueuedTimeUtc().toString());
            schemaChangedMessages.setMessageId(messageId);

            indexUpdateMessageHandler.sendSchemaChangedMessagesToIndexer(schemaChangedMessages);
        } finally {
            MDC.clear();
            ThreadScopeContextHolder.getContext().clear();
        }
    }
}
