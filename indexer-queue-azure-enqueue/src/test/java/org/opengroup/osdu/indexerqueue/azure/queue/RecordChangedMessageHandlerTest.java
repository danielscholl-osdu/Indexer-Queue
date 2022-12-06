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
import org.junit.Before;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.cache.VmCache;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.indexerqueue.azure.di.AzureBootstrapConfig;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@ExtendWith(MockitoExtension.class)
public class RecordChangedMessageHandlerTest {
    private static final String indexerWorkerUrl = "indexer-worker-url";
    private static final int maxTry = 5;
    private final String ACCOUNT_ID = "test-tenant";
    private final String CORRELATION_ID = "xxxxxx";
    private static MockedConstruction<HttpPost> httpMock;

    @Mock
    private AzureBootstrapConfig azureBootstrapConfig;
    @Mock
    private Gson gson;
    @Mock
    private RecordChangedMessages recordChangedMessages;
    @Mock
    private CloseableHttpResponse httpResponse;
    @Mock
    private CloseableHttpClient httpClient;
    @Mock
    private HttpClientBuilder httpClientBuilder;
    @InjectMocks
    private RecordChangedMessageHandler sut;

    @BeforeEach
    public void setup() {
        Map<String, String> headers = new HashMap<>();
        headers.put(DpsHeaders.ACCOUNT_ID, ACCOUNT_ID);
        headers.put(DpsHeaders.CORRELATION_ID, CORRELATION_ID);
        recordChangedMessages = RecordChangedMessages.builder()
                .attributes(headers).build();

        httpMock = mockConstruction(
          HttpPost.class,(mock, context) -> {
            doNothing().when(mock).setEntity(any(HttpEntity.class));
            doNothing().when(mock).setHeader(any());
        });
    }

    @AfterEach
    public void close() {
      httpMock.close();
    }

    @Test
    public void shouldThrow_whenHttpClientThrows() throws Exception{
        RuntimeException exp = new RuntimeException("httpClientBuilder build failed");
        when(httpClientBuilder.build()).thenThrow(exp);
        try {
            sut.sendMessagesToIndexer(recordChangedMessages);
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
            sut.sendMessagesToIndexer(recordChangedMessages);
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
        when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
        StatusLine status = new BasicStatusLine(new ProtocolVersion("http",1,1),200,"success");
        when(httpResponse.getStatusLine()).thenReturn(status);

        sut.sendMessagesToIndexer(recordChangedMessages);

        assertEquals(httpMock.constructed().size(),1);
        verify(httpClient,times(1)).execute(any());
        verify(azureBootstrapConfig, times(1)).getIndexerWorkerURL();
    }

    @Test
    public void shouldThrow_whenHttpPostMethod_ReturnsErrorResponseCode() throws Exception {
        when(httpClientBuilder.build()).thenReturn(httpClient);
        when(azureBootstrapConfig.getIndexerWorkerURL()).thenReturn(indexerWorkerUrl);
        when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
        StatusLine status = new BasicStatusLine(new ProtocolVersion("http",1,1),400,"error");
        when(httpResponse.getStatusLine()).thenReturn(status);

        try {
            sut.sendMessagesToIndexer(recordChangedMessages);
        } catch (Exception e) {
            assertEquals(httpMock.constructed().size(),1);
            verify(httpClient,times(1)).execute(any());
            verify(azureBootstrapConfig, times(1)).getIndexerWorkerURL();
        }
    }
}
