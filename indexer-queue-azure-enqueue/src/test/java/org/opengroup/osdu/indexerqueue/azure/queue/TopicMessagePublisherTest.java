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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.servicebus.ITopicClientFactory;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.springframework.retry.support.RetryTemplate;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TopicMessagePublisherTest {
    private static final String PARTITION_ID = "testPartitionId";
    private static final String TOPIC_NAME = "topicName";

    @Mock
    private ITopicClientFactory topicClientFactory;

    private RetryTemplate retryTemplate = new RetryTemplate();

    private MessagePublisher messagePublisher;

    @Mock
    private TopicClient topicClient;
    @Mock
    private IMessage message;
    private Instant instant = Instant.now();

    @BeforeEach
    public void setup() {
        messagePublisher = new TopicMessagePublisher(topicClientFactory, retryTemplate, PARTITION_ID, TOPIC_NAME);
    }

    @Test
    public void shouldSendMessageToTopic() throws ServiceBusException, InterruptedException {
        when(topicClientFactory.getClient(PARTITION_ID, TOPIC_NAME)).thenReturn(topicClient);

        messagePublisher.sendMessageToTopic(message, instant);

        verify(topicClient, only()).scheduleMessageAsync(message, instant);
    }

    @Test
    public void shouldThrowAppExceptionOnInterruptedException() throws ServiceBusException, InterruptedException {
        when(topicClientFactory.getClient(PARTITION_ID, TOPIC_NAME)).thenThrow(new InterruptedException());

        assertThrows(AppException.class, () -> messagePublisher.sendMessageToTopic(message, instant));
    }

    @Test
    public void shouldThrowAppExceptionOnServiceBusException() throws ServiceBusException, InterruptedException {
        when(topicClientFactory.getClient(PARTITION_ID, TOPIC_NAME)).thenThrow(new ServiceBusException(false));

        assertThrows(AppException.class, () -> messagePublisher.sendMessageToTopic(message, instant));
    }
}
