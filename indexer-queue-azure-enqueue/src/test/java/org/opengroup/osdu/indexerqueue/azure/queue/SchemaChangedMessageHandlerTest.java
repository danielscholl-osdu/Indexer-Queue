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

import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.MessageBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.indexer.SchemaChangedMessages;
import org.opengroup.osdu.indexerqueue.azure.config.ThreadDpsHeaders;
import org.opengroup.osdu.indexerqueue.azure.util.MdcContextMap;
import org.opengroup.osdu.indexerqueue.azure.util.MessageAttributesExtractor;
import org.opengroup.osdu.indexerqueue.azure.util.SchemaChangedAttributes;
import org.opengroup.osdu.indexerqueue.azure.util.SchemaChangedSbMessageBuilder;

import java.time.Instant;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SchemaChangedMessageHandlerTest {

    private static final String SCHEMA_INFO_PAYLOAD = "[{\"kind\":\"testKind\", \"op\": \"testOp\"}]";

    @InjectMocks
    private SchemaChangedMessageHandler sut;
    @Mock
    private IndexUpdateMessageHandler indexUpdateMessageHandler;
    @Mock
    private SchemaChangedSbMessageBuilder schemaChangedSbMessageBuilder;
    @Mock
    private ThreadDpsHeaders dpsHeaders;
    @Mock
    private MdcContextMap mdcContextMap;
    @Mock
    private MessageAttributesExtractor messageAttributesExtractor;
    @Mock
    private Message message;

    private SchemaChangedMessages schemaChangedMessages = new SchemaChangedMessages();
    private MessageBody messageBody = new Message().getMessageBody();

    @BeforeEach
    public void init() {
        schemaChangedMessages.setData(SCHEMA_INFO_PAYLOAD);
        when(schemaChangedSbMessageBuilder.buildSchemaChangedServiceBusMessage(anyString())).thenReturn(schemaChangedMessages);
        when(messageAttributesExtractor.extractSchemaChangedAttributesFromMessageBody(any())).thenReturn(new SchemaChangedAttributes());
        when(message.getEnqueuedTimeUtc()).thenReturn(Instant.now());
        when(message.getMessageId()).thenReturn(EMPTY);
        when(message.getMessageBody()).thenReturn(messageBody);
    }

    @Test
    public void should_Invoke_SendMessagesToIndexer() {
        // Execute
        sut.processMessage(message);

        // Verify
        verify(indexUpdateMessageHandler, times(1)).sendSchemaChangedMessagesToIndexer(schemaChangedMessages);
        verify(message, times(1)).getMessageId();
        verify(message, times(1)).getMessageBody();
        verify(message, times(1)).getEnqueuedTimeUtc();
    }

    @Test
    public void shouldThrow_whenSendMessagesToIndexerThrows() {
        //Setup
        RuntimeException exp = new RuntimeException("httpClientBuilder build failed");
        doThrow(exp).when(indexUpdateMessageHandler).sendSchemaChangedMessagesToIndexer(schemaChangedMessages);

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
