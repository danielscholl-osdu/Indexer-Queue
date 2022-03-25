package org.opengroup.osdu.indexerqueue.azure.queue;

import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.MessageBody;
import com.microsoft.azure.servicebus.SubscriptionClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.indexerqueue.azure.util.SbMessageBuilder;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class MessageHandlerTest {
    @InjectMocks
    private MessageHandler sut;
    @Mock
    private SubscriptionClient subscriptionClient;
    @Mock
    private RecordChangedMessageHandler recordChangedMessageHandler;
    @Mock
    private SbMessageBuilder sbMessageBuilder;
    @Mock
    private Message message;

    private static final UUID uuid = UUID.randomUUID();
    private RecordChangedMessages recordChangedMessages = new RecordChangedMessages();
    private MessageBody messageBody = new Message().getMessageBody();
    private static final long currentTry = 4L;

    @BeforeEach
    public void init() throws IOException {
        when(sbMessageBuilder.getServiceBusMessage(anyString(), anyString())).thenReturn(recordChangedMessages);
        when(message.getEnqueuedTimeUtc()).thenReturn(Instant.now());
        when(message.getMessageId()).thenReturn(new String());
        when(message.getMessageBody()).thenReturn(messageBody);
        when(message.getDeliveryCount()).thenReturn(currentTry);
    }

    @Test
    public void should_Invoke_SendMessagesToIndexer() throws Exception {
        // Execute
        sut.processMessage(message);

        // Verify
        verify(recordChangedMessageHandler, times(1)).sendMessagesToIndexer(recordChangedMessages, currentTry);
        verify(message, times(1)).getDeliveryCount();
        verify(message, times(1)).getMessageId();
        verify(message, times(1)).getMessageBody();
        verify(message, times(2)).getEnqueuedTimeUtc();
    }

    @Test
    public void shouldThrow_whenSendMessagesToIndexerThrows() throws Exception{
        //Setup
        Exception exp = new Exception("httpClientBuilder build failed");
        doThrow(exp).when(recordChangedMessageHandler).sendMessagesToIndexer(recordChangedMessages, currentTry);

        // Execute
        try {
            sut.processMessage(message);
        }
        catch (Exception e) {
        // Verify
            assertEquals("httpClientBuilder build failed", e.getMessage());
        }
    }
}
