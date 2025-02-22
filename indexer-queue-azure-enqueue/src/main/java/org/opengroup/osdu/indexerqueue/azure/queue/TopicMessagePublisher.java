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
import com.microsoft.azure.servicebus.TopicClient;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.opengroup.osdu.azure.servicebus.ITopicClientFactory;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.springframework.retry.support.RetryTemplate;

import java.time.Instant;

/**
 * Implementation of the message publisher interface.
 */
public class TopicMessagePublisher implements MessagePublisher {

    private final ITopicClientFactory topicClientFactory;
    private final RetryTemplate retryTemplate;
    private final String partitionId;
    private final String topicName;

    /**
     * @param factory - factory object for topicClient
     * @param template - retry template for topicClient
     * @param dataPartitionId - data partition id
     * @param topic - topic name
     */
    public TopicMessagePublisher(final ITopicClientFactory factory,
                                 final RetryTemplate template,
                                 final String dataPartitionId,
                                 final String topic) {
        this.topicClientFactory = factory;
        this.retryTemplate = template;
        this.partitionId = dataPartitionId;
        this.topicName = topic;
    }

    /**
     *
     * @param message - service bus message
     * @param enqueueTimeUtc - enqueue time for the message
     */
    @Override
    public void sendMessageToTopic(final IMessage message, final Instant enqueueTimeUtc) {
        TopicClient topicClient;
        try {
            topicClient = topicClientFactory.getClient(partitionId, topicName);
            retryTemplate.execute(arg -> {
                topicClient.scheduleMessageAsync(message, enqueueTimeUtc);
                return null;
            });
        } catch (ServiceBusException | InterruptedException e) {
            throw new AppException(500, "Server Error", "Unexpected error creating Topic Client", e);
        }
    }
}
