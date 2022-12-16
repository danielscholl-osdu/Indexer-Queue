package org.opengroup.osdu.indexerqueue.azure.queue;

import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.MessageBody;
import com.microsoft.azure.servicebus.SubscriptionClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.indexerqueue.azure.metrics.IMetricService;
import org.opengroup.osdu.indexerqueue.azure.util.SbMessageBuilder;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MessageHandlerTest {

    private static final String RECORD_INFO_PAYLOAD = "[{\"id\":\"testId\", \"kind\":\"testKind\", \"op\": \"testOp\"}]";

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
    @Mock
    private IMetricService metricService;

    private static final UUID uuid = UUID.randomUUID();
    private RecordChangedMessages recordChangedMessages = new RecordChangedMessages();
    private MessageBody messageBody = new Message().getMessageBody();

    @BeforeEach
    public void init() throws IOException {
        recordChangedMessages.setData(RECORD_INFO_PAYLOAD);
        when(sbMessageBuilder.getServiceBusMessage(anyString(), anyString())).thenReturn(recordChangedMessages);
        when(message.getEnqueuedTimeUtc()).thenReturn(Instant.now());
        when(message.getMessageId()).thenReturn(new String());
        when(message.getMessageBody()).thenReturn(messageBody);
    }

    @Test
    public void should_Invoke_SendMessagesToIndexer() throws Exception {
        // Execute
        sut.processMessage(message);

        // Verify
        verify(recordChangedMessageHandler, times(1)).sendMessagesToIndexer(recordChangedMessages);
        verify(message, times(1)).getMessageId();
        verify(message, times(1)).getMessageBody();
        verify(message, times(2)).getEnqueuedTimeUtc();
    }

    @Test
    public void shouldThrow_whenSendMessagesToIndexerThrows() throws Exception{
        //Setup
        RuntimeException exp = new RuntimeException("httpClientBuilder build failed");
        doThrow(exp).when(recordChangedMessageHandler).sendMessagesToIndexer(recordChangedMessages);

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
