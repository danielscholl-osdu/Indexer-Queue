package org.opengroup.osdu.indexerqueue.azure.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.indexer.SchemaChangedMessages;
import org.opengroup.osdu.indexerqueue.azure.config.ThreadDpsHeaders;

import java.io.IOException;

@ExtendWith(MockitoExtension.class)
public class SchemaChangedSbMessageBuilderTest {
    private final String requestBodyInvalidJson = "";
    private final String requestBodyEmpty = "{}";
    private final String requestBodyValid = "{\"message\":{\"data\":[{\"kind\":\"common:welldb:wellbore:1.0.0\",\"op\":\"create\"}],\"account-id\":\"common\",\"data-partition-id\":\"common\",\"correlation-id\":\"ee85038e-4510-49d9-b2ec-3651315a4d00\"}}";
    private final String requestBodyMissingData = "{\"message\":{\"account-id\":\"common\",\"data-partition-id\":\"common\",\"correlation-id\":\"ee85038e-4510-49d9-b2ec-3651315a4d00\"}}";
    private final String requestBodyMissingTenantId = "{\"message\":{\"data\":[{\"kind\":\"common:welldb:wellbore:1.0.0\",\"op\":\"create\"}],\"account-id\":\"common\",\"correlation-id\":\"ee85038e-4510-49d9-b2ec-3651315a4d00\"}}";

    @Mock
    private ThreadDpsHeaders dpsHeaders;
    @Mock
    private MdcContextMap mdcContextMap;

    @InjectMocks
    private SchemaChangedSbMessageBuilder sut;

    @Test
    public void shouldThrow_ForEmptyRequestBody() throws IOException {
        try {
            sut.buildSchemaChangedServiceBusMessage(requestBodyEmpty);
        }
        catch (AppException e) {
            Assertions.assertEquals(e.getMessage(), "message object not found");
        }
    }

    @Test
    public void shouldThrow_ForRequestBodyWith_NoData() throws IOException {
        try {
            sut.buildSchemaChangedServiceBusMessage(requestBodyMissingData);
        }
        catch (AppException e) {
            Assertions.assertEquals(e.getMessage(), "message data not found");
        }
    }

    @Test
    public void shouldThrow_ForInvalidJsonInRequest() throws IOException {
        try {
            sut.buildSchemaChangedServiceBusMessage(requestBodyInvalidJson);
        }
        catch (AppException e) {
            Assertions.assertEquals(e.getMessage(),"Could not fetch JSON object");
        }
    }

    @Test
    public void shouldThrow_ForMissingTenant() throws IOException {
        try {
            sut.buildSchemaChangedServiceBusMessage(requestBodyMissingTenantId);
        }
        catch (AppException e) {
            Assertions.assertEquals(e.getMessage(),"tenant-id missing");
        }
    }

    @Test
    public void shouldReturn_ValidRecordChangedMessage() throws IOException {
        String expectedCorrelationId = "ee85038e-4510-49d9-b2ec-3651315a4d00";
        String expectedDataPartitionId = "common";
        String expectedData = "[{\"kind\":\"common:welldb:wellbore:1.0.0\",\"op\":\"create\"}]";

        SchemaChangedMessages schemaChangedMessages = sut.buildSchemaChangedServiceBusMessage(requestBodyValid);

        Assertions.assertEquals(expectedCorrelationId, schemaChangedMessages.getCorrelationId());
        Assertions.assertEquals(expectedDataPartitionId, schemaChangedMessages.getDataPartitionId());
        Assertions.assertNotNull(schemaChangedMessages.getData());
        Assertions.assertEquals(expectedData, schemaChangedMessages.getData());
    }
}
