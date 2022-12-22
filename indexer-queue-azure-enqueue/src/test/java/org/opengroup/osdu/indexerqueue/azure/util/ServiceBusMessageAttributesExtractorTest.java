package org.opengroup.osdu.indexerqueue.azure.util;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.MessageBody;

@ExtendWith(MockitoExtension.class)
public class ServiceBusMessageAttributesExtractorTest {

    private static final String CORRELATION_ID = "d84f0c58-f2c6-446f-b246-d79ccdb6a4bd";
    private static final String DATA_PARTITION_ID = "opendes";
    private static final String CORRECT_MESSAGE_BODY = "{\n"
      + "    \"message\": {\n"
      + "        \"data\": [\n"
      + "            {\n"
      + "                \"id\": \"testId\",\n"
      + "                \"kind\": \"testKind\",\n"
      + "                \"op\": \"update\"\n"
      + "            }\n"
      + "        ],\n"
      + "        \"data-partition-id\": \"" + DATA_PARTITION_ID + "\",\n"
      + "        \"correlation-id\": \"" + CORRELATION_ID + "\"\n"
      + "    }\n"
      + "}";

  private static final String INCORRECT_MESSAGE_BODY = "{\n"
    + "    \"message\": \"wrong\""
    + "}";

    private final ServiceBusMessageAttributesExtractor extractor = new ServiceBusMessageAttributesExtractor();
    @Mock
    private IMessage message;
    @Mock
    private MessageBody messageBody;

    @BeforeEach
    public void setup() {
        when(message.getMessageBody()).thenReturn(messageBody);
    }

    @Test
    public void shouldExtractAttributesSuccessfully() {
        when(messageBody.getBinaryData()).thenReturn(singletonList(CORRECT_MESSAGE_BODY.getBytes(UTF_8)));

        RecordChangedAttributes recordChangedAttributes = extractor.extractAttributesFromMessageBody(message);

        assertEquals(CORRELATION_ID, recordChangedAttributes.getCorrelationId());
        assertEquals(DATA_PARTITION_ID, recordChangedAttributes.getDataPartitionId());
    }

    @Test
    public void shouldNotFailIfMessageBodyIsEmpty() {
        when(messageBody.getBinaryData()).thenReturn(singletonList(EMPTY.getBytes(UTF_8)));

        RecordChangedAttributes recordChangedAttributes = extractor.extractAttributesFromMessageBody(message);

        assertNull(recordChangedAttributes.getDataPartitionId(), recordChangedAttributes.getCorrelationId());
    }

    @Test
    public void shouldNotFailIfMessageBodyHasIncorrectStructure() {
        when(messageBody.getBinaryData()).thenReturn(singletonList(INCORRECT_MESSAGE_BODY.getBytes(UTF_8)));

        RecordChangedAttributes recordChangedAttributes = extractor.extractAttributesFromMessageBody(message);

        assertNull(recordChangedAttributes.getDataPartitionId(), recordChangedAttributes.getCorrelationId());
    }
}
