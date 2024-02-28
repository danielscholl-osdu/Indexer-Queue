package org.opengroup.osdu.indexerqueue.azure.queue;

import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.MessageBody;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.indexerqueue.azure.util.RecordsChangedSbMessageBuilder;

import java.io.IOException;
import java.time.Instant;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RecordChangedMessageHandlerTest {

    private static final String RECORD_INFO_PAYLOAD = "[{\"id\":\"testId\", \"kind\":\"testKind\", \"op\": \"testOp\"}]";

    @InjectMocks
    private RecordChangedMessageHandler sut;
    @Mock
    private IndexUpdateMessageHandler indexUpdateMessageHandler;
    @Mock
    private RecordsChangedSbMessageBuilder recordsChangedSbMessageBuilder;
    @Mock
    private Message message;

    private RecordChangedMessages recordChangedMessages = new RecordChangedMessages();
    private MessageBody messageBody = new Message().getMessageBody();

    @BeforeEach
    public void init() throws IOException {
        recordChangedMessages.setData(RECORD_INFO_PAYLOAD);
        when(recordsChangedSbMessageBuilder.getServiceBusMessage(anyString(), anyString())).thenReturn(recordChangedMessages);
        when(message.getEnqueuedTimeUtc()).thenReturn(Instant.now());
        when(message.getMessageId()).thenReturn(EMPTY);
        when(message.getMessageBody()).thenReturn(messageBody);
    }

    @Test
    public void should_Invoke_SendMessagesToIndexer() throws Exception {
        // Execute
        sut.processMessage(message);

        // Verify
        verify(indexUpdateMessageHandler, times(1)).sendRecordChangedMessagesToIndexer(recordChangedMessages);
        verify(message, times(1)).getMessageId();
        verify(message, times(1)).getMessageBody();
        verify(message, times(1)).getEnqueuedTimeUtc();
    }

    @Test
    public void shouldThrow_whenSendMessagesToIndexerThrows() throws Exception{
        //Setup
        RuntimeException exp = new RuntimeException("httpClientBuilder build failed");
        doThrow(exp).when(indexUpdateMessageHandler).sendRecordChangedMessagesToIndexer(recordChangedMessages);

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
