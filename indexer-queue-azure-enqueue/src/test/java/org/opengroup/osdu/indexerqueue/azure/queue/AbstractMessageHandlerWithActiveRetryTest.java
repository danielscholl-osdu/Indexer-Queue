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
import com.microsoft.azure.servicebus.MessageBody;
import com.microsoft.azure.servicebus.SubscriptionClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.indexerqueue.azure.util.RetryUtil;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AbstractMessageHandlerWithActiveRetryTest {

    private static final String PROPERTY_RETRY = "RETRY";
    private static final String WORKER_NAME = "worker_name";
    private static final Integer MAX_DELIVERY_COUNT = 5;
    private static final UUID UUID = java.util.UUID.randomUUID();
    private static final Instant INSTANT = Instant.now();
    private static final String TEST_MESSAGE_BODY = "testMessageBody";
    private Map<String, Object> messageProperties;

    @Mock
    private SubscriptionClient receiveClient;

    @Mock
    private MessagePublisher messagePublisher;

    @Mock
    private RetryUtil retryUtil;

    @Mock
    private TestMessageProcessor testMessageProcessor;

    @Mock
    private IMessage message;

    @Mock
    private MessageBody messageBody;

    private AbstractMessageHandlerWithActiveRetry messageHandler;

    @BeforeEach
    public void setup() {
        messageProperties = new HashMap<>();
        messageHandler = new AbstractMessageHandlerWithActiveRetry(receiveClient,
                messagePublisher, retryUtil, WORKER_NAME, MAX_DELIVERY_COUNT) {
            @Override
            public void processMessage(IMessage message) {
                testMessageProcessor.doTheProcessing(message);
            }
        };
        when(message.getEnqueuedTimeUtc()).thenReturn(INSTANT);
    }

    @Test
    public void testShouldProcessMessagesSuccessfully() {
        when(message.getLockToken()).thenReturn(UUID);
        messageHandler.onMessageAsync(message);

        verify(testMessageProcessor, only()).doTheProcessing(message);
        verify(receiveClient, only()).completeAsync(UUID);
        verify(message, times(1)).getLockToken();
        verify(message, times(1)).getEnqueuedTimeUtc();
        verify(message, times(1)).getMessageId();
        verifyNoInteractions(messagePublisher);
    }

    @Test
    public void should_completeMessage_ifExceptionIsThrown_andRetryPropertyIsEmpty() {
        setupMessagesStubsForFailureCases();
        when(message.getLockToken()).thenReturn(UUID);
        when(retryUtil.generateNextRetryTerm(1)).thenReturn(1);
        doThrow(new RuntimeException()).when(testMessageProcessor).doTheProcessing(message);

        messageHandler.onMessageAsync(message);

        assertEquals(1, messageProperties.get(PROPERTY_RETRY));

        verify(receiveClient, only()).completeAsync(UUID);
        verify(messagePublisher, only()).sendMessageToTopic(eq(message), any());
    }

    @Test
    public void should_completeMessage_ifExceptionIsThrown_andRetryPropertyIsNotEmpty() {
        setupMessagesStubsForFailureCases();
        when(message.getLockToken()).thenReturn(UUID);
        messageProperties.put(PROPERTY_RETRY, 1);
        when(retryUtil.generateNextRetryTerm(2)).thenReturn(2);
        doThrow(new RuntimeException()).when(testMessageProcessor).doTheProcessing(message);

        messageHandler.onMessageAsync(message);

        assertEquals(2, messageProperties.get(PROPERTY_RETRY));

        verify(receiveClient, only()).completeAsync(UUID);
        verify(messagePublisher, only()).sendMessageToTopic(eq(message), any());
    }

    @Test
    public void should_deadLetterMessage_ifExceptionIsThrown_andRetryPropertyMatchesMaxDeliveryCount() {
        setupMessagesStubsForFailureCases();
        when(message.getLockToken()).thenReturn(UUID);
        messageProperties.put(PROPERTY_RETRY, MAX_DELIVERY_COUNT + 1);
        doThrow(new RuntimeException("test msg")).when(testMessageProcessor).doTheProcessing(message);

        messageHandler.onMessageAsync(message);

        assertEquals(6, messageProperties.get(PROPERTY_RETRY));

        verify(receiveClient, only()).deadLetterAsync(UUID);
        verifyNoInteractions(messagePublisher);
    }

    @Test
    public void should_completeMessageAndDeadLetter_ifMessageProcessorFailed_andMessagePublisherThrowsException() {
        setupMessagesStubsForFailureCases();
        when(message.getLockToken()).thenReturn(UUID);
        doThrow(new RuntimeException()).when(testMessageProcessor).doTheProcessing(message);
        doThrow(new RuntimeException()).when(messagePublisher).sendMessageToTopic(eq(message), any());

        messageHandler.onMessageAsync(message);

        assertEquals(1, messageProperties.get(PROPERTY_RETRY));

        verify(receiveClient, times(1)).completeAsync(UUID);
        verify(receiveClient, times(1)).deadLetterAsync(UUID);
        verify(messagePublisher, only()).sendMessageToTopic(eq(message), any());
    }

    private void setupMessagesStubsForFailureCases() {
        when(messageBody.getBinaryData()).thenReturn(singletonList(TEST_MESSAGE_BODY.getBytes(UTF_8)));
        when(message.getEnqueuedTimeUtc()).thenReturn(INSTANT);
        when(message.getProperties()).thenReturn(messageProperties);
        when(message.getMessageBody()).thenReturn(messageBody);
    }

    // Needed to control invocations and throws exceptions
    private static class TestMessageProcessor {
        public void doTheProcessing(IMessage message) {
        }
    }
}
