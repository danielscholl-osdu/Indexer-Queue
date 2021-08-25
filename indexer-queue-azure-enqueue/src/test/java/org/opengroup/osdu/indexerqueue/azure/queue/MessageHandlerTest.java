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

import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Matchers.any;
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
    private static final long maxDeliveryCountLimit = 5L;
    private static final long currentTry = 4L;

    @BeforeEach
    public void init() throws IOException {
        when(sbMessageBuilder.getServiceBusMessage(anyString())).thenReturn(recordChangedMessages);
        when(message.getEnqueuedTimeUtc()).thenReturn(Instant.now());
        when(message.getMessageId()).thenReturn(new String());
        when(message.getMessageBody()).thenReturn(messageBody);
        when(message.getDeliveryCount()).thenReturn(currentTry);
    }

    @Test
    public void should_AbandonCompletion_ifExceptionIsThrown_beforeMessageLockExpiry() throws Exception {
        // Setup
        // Message Expiry Time Instant > Current Instant
        when(message.getExpiresAtUtc()).thenReturn(Instant.now().plus(1, ChronoUnit.DAYS));
        when(message.getLockToken()).thenReturn(uuid);
        Mockito.doThrow(new Exception()).when(recordChangedMessageHandler).sendMessagesToIndexer(recordChangedMessages, currentTry);

        // Execute
        sut.onMessageAsync(message);

        // Verify
        verify(subscriptionClient, times(1)).abandonAsync(uuid);
        verify(recordChangedMessageHandler, times(1)).sendMessagesToIndexer(recordChangedMessages, currentTry);
        verify(message, times(1)).getDeliveryCount();
        verify(message, times(1)).getExpiresAtUtc();
        verify(message, times(2)).getEnqueuedTimeUtc();
        verify(message, times(1)).getMessageId();
        verify(message, times(1)).getMessageBody();
        verify(message, times(1)).getLockToken();
    }

    @Test
    public void shouldReturnNull_ifExceptionIsThrown_atOrAfterMessageLockExpiry() throws Exception {
        // Setup
        // message lock expiry time was before current instant
        when(message.getExpiresAtUtc()).thenReturn(Instant.now().minus(1, ChronoUnit.DAYS));
        Mockito.doThrow(new Exception()).when(recordChangedMessageHandler).sendMessagesToIndexer(recordChangedMessages, currentTry);

        // Execute
        CompletableFuture<Void> returnValue =  sut.onMessageAsync(message);

        // Verify
        verify(recordChangedMessageHandler, times(1)).sendMessagesToIndexer(recordChangedMessages, currentTry);
        verify(message, times(1)).getDeliveryCount();
        verify(message, times(1)).getExpiresAtUtc();
        verify(message, times(1)).getMessageId();
        verify(message, times(1)).getMessageBody();
        verify(message, times(2)).getEnqueuedTimeUtc();
        assertNull(returnValue);
    }

    @Test
    public void should_Invoke_CompleteAsync() throws Exception {
        // Setup
        when(message.getLockToken()).thenReturn(uuid);

        // Execute
        sut.onMessageAsync(message);

        // Verify
        verify(subscriptionClient, times(1)).completeAsync(uuid);
        verify(recordChangedMessageHandler, times(1)).sendMessagesToIndexer(recordChangedMessages, currentTry);
        verify(message, times(1)).getDeliveryCount();
        verify(message, times(1)).getMessageId();
        verify(message, times(1)).getMessageBody();
        verify(message, times(1)).getLockToken();
        verify(message, times(2)).getEnqueuedTimeUtc();
    }
}
