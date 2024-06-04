package org.opengroup.osdu.indexerqueue.azure.queue;

import com.google.gson.Gson;
import org.apache.http.HttpEntity;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicStatusLine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.http.RequestStatus;
import org.opengroup.osdu.core.common.model.indexer.SchemaChangedMessages;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.core.common.util.IServiceAccountJwtClient;
import org.opengroup.osdu.indexerqueue.azure.di.AzureBootstrapConfig;
import org.opengroup.osdu.indexerqueue.azure.exceptions.IndexerNoRetryException;
import org.opengroup.osdu.indexerqueue.azure.exceptions.IndexerRetryException;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class IndexUpdateMessageHandlerTest {
    private static final String indexerWorkerUrl = "indexer-worker-url";
    private static final String schemaWorkerUrl = "schema-worker-url";
    private static final int maxTry = 5;
    private final String DATA_PARTITION_ID = "test-tenant";
    private final String CORRELATION_ID = "xxxxxx";
    private static MockedConstruction<HttpPost> httpMock;

    @Mock
    private AzureBootstrapConfig azureBootstrapConfig;
    @Mock
    private Gson gson;
    @Mock
    private RecordChangedMessages recordChangedMessages;
    @Mock
    private SchemaChangedMessages schemaChangedMessages;
    @Mock
    private CloseableHttpResponse httpResponse;
    @Mock
    private CloseableHttpClient httpClient;
    @Mock
    private HttpClientBuilder httpClientBuilder;
    @Mock
    private IServiceAccountJwtClient serviceAccountJwtClient;
    @InjectMocks
    private IndexUpdateMessageHandler sut;

    @BeforeEach
    public void setup() {
        Map<String, String> headers = new HashMap<>();
        headers.put(DpsHeaders.DATA_PARTITION_ID, DATA_PARTITION_ID);
        headers.put(DpsHeaders.CORRELATION_ID, CORRELATION_ID);
        recordChangedMessages = RecordChangedMessages.builder()
                .attributes(headers).build();

        httpMock = mockConstruction(
                HttpPost.class, (mock, context) -> {
                    doNothing().when(mock).setEntity(any(HttpEntity.class));
                    doNothing().when(mock).setHeader(any());
                });
    }

    @AfterEach
    public void close() {
        httpMock.close();
    }

    @Test
    public void shouldThrow_whenHttpClientThrowsException() throws Exception {
        RuntimeException exp = new RuntimeException("httpClientBuilder build failed");
        when(httpClientBuilder.build()).thenThrow(exp);
        try {
            sut.sendRecordChangedMessagesToIndexer(recordChangedMessages);
        }
        catch (Exception e) {
            assertEquals("httpClientBuilder build failed", e.getMessage());
        }

        try {
            sut.sendSchemaChangedMessagesToIndexer(schemaChangedMessages);
        }
        catch (Exception e) {
            assertEquals("httpClientBuilder build failed", e.getMessage());
        }
    }

    @Test
    public void shouldNot_BackoffExponentially_whenHttpClientThrows_andCurrentTryEqualToMaxTry() throws Exception {
        RuntimeException exp = new RuntimeException("httpClientBuilder build failed");
        when(httpClientBuilder.build()).thenThrow(exp);
        java.sql.Timestamp before = null;
        long backOffWaitTime = ((long) Math.pow(2, maxTry) * 10L);

        try {
            before = new java.sql.Timestamp(new Date().getTime());
            sut.sendRecordChangedMessagesToIndexer(recordChangedMessages);
        } catch (Exception e) {
            java.sql.Timestamp after = new java.sql.Timestamp(new Date().getTime());
            long actualWaitTime = after.getTime() - before.getTime();
            assertTrue(actualWaitTime < backOffWaitTime);
        }

        try {
            before = new java.sql.Timestamp(new Date().getTime());
            sut.sendSchemaChangedMessagesToIndexer(schemaChangedMessages);
        } catch (Exception e) {
            java.sql.Timestamp after = new java.sql.Timestamp(new Date().getTime());
            long actualWaitTime = after.getTime() - before.getTime();
            assertTrue(actualWaitTime < backOffWaitTime);
        }
    }

    @Test
    public void shouldInvoke_httpPostMethod_whenHttpResponseCodeIsSuccess() throws Exception {
        when(httpClientBuilder.build()).thenReturn(httpClient);
        when(azureBootstrapConfig.getIndexerWorkerURL()).thenReturn(indexerWorkerUrl);
        when(azureBootstrapConfig.getSchemaWorkerURL()).thenReturn(schemaWorkerUrl);
        when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
        StatusLine status = new BasicStatusLine(new ProtocolVersion("http",1,1),200,"success");
        when(httpResponse.getStatusLine()).thenReturn(status);

        sut.sendRecordChangedMessagesToIndexer(recordChangedMessages);
        sut.sendSchemaChangedMessagesToIndexer(schemaChangedMessages);

        assertEquals(httpMock.constructed().size(),2);
        verify(httpClient,times(2)).execute(any());
        verify(azureBootstrapConfig, times(1)).getIndexerWorkerURL();
        verify(azureBootstrapConfig, times(1)).getSchemaWorkerURL();
        verify(serviceAccountJwtClient, times(2)).getIdToken(any());
    }

    @Test
    public void shouldThrow_whenHttpPostMethod_ReturnsErrorResponseCode() throws Exception {
        when(httpClientBuilder.build()).thenReturn(httpClient);
        when(azureBootstrapConfig.getIndexerWorkerURL()).thenReturn(indexerWorkerUrl);
        when(azureBootstrapConfig.getSchemaWorkerURL()).thenReturn(schemaWorkerUrl);
        when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
        StatusLine status = new BasicStatusLine(new ProtocolVersion("http", 1, 1), 400, "error");
        when(httpResponse.getStatusLine()).thenReturn(status);

        try {
            sut.sendRecordChangedMessagesToIndexer(recordChangedMessages);
        } catch (Exception e) {
            assertEquals(httpMock.constructed().size(), 1);
            verify(httpClient, times(1)).execute(any());
            verify(azureBootstrapConfig, times(1)).getIndexerWorkerURL();
            verify(serviceAccountJwtClient, times(1)).getIdToken(any());
        }

        try {
            sut.sendSchemaChangedMessagesToIndexer(schemaChangedMessages);
        } catch (IndexerRetryException e) {
            assertEquals(httpMock.constructed().size(),2);
            verify(httpClient,times(2)).execute(any());
            verify(azureBootstrapConfig, times(1)).getSchemaWorkerURL();
        } catch (Exception e) {
            fail("Should only throw IndexerRetryException");
        }
    }

    @Test
    public void shouldThrow_whenHttpPostMethod_ReturnsNoRetryResponseCode() throws Exception {
        when(httpClientBuilder.build()).thenReturn(httpClient);
        when(azureBootstrapConfig.getIndexerWorkerURL()).thenReturn(indexerWorkerUrl);
        when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
        StatusLine status = new BasicStatusLine(new ProtocolVersion("http",1,1), RequestStatus.NO_RETRY,"error");
        when(httpResponse.getStatusLine()).thenReturn(status);

        assertThrows(IndexerNoRetryException.class, () -> sut.sendRecordChangedMessagesToIndexer(recordChangedMessages));
        verify(serviceAccountJwtClient, times(1)).getIdToken(any());
    }

    @Test
    public void shouldThrowIndexerNoRetryException_whenSchemaWorker_Returns500() throws Exception {
        when(httpClientBuilder.build()).thenReturn(httpClient);
        when(azureBootstrapConfig.getSchemaWorkerURL()).thenReturn(schemaWorkerUrl);
        when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
        StatusLine status = new BasicStatusLine(new ProtocolVersion("http",1,1),500,"error");
        when(httpResponse.getStatusLine()).thenReturn(status);
        try {
            sut.sendSchemaChangedMessagesToIndexer(schemaChangedMessages);
        } catch (IndexerNoRetryException e) {
            assertEquals(httpMock.constructed().size(),1);
            verify(httpClient,times(1)).execute(any());
            verify(azureBootstrapConfig, times(1)).getSchemaWorkerURL();
            verify(serviceAccountJwtClient, times(1)).getIdToken(any());
        } catch (Exception e) {
            fail("Should only throw IndexerNoRetryException");
        }
    }
}
