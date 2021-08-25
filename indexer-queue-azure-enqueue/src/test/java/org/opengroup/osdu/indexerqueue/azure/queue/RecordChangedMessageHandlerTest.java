package org.opengroup.osdu.indexerqueue.azure.queue;

import com.google.gson.Gson;
import org.apache.http.HttpEntity;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicStatusLine;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@RunWith(PowerMockRunner.class)
@PrepareForTest({RecordChangedMessageHandler.class})
@PowerMockIgnore({"javax.net.ssl.*", "javax.management.*"})
public class RecordChangedMessageHandlerTest {
    private static final String indexerWorkerUrl = "indexer-worker-url";
    private static final int currentTry = 1;
    private static final int maxTry = 5;
    private final String ACCOUNT_ID = "test-tenant";
    private final String CORRELATION_ID = "xxxxxx";

    @Mock
    private AzureBootstrapConfig azureBootstrapConfig;
    @Mock
    private Gson gson;
    @Mock
    private RecordChangedMessages recordChangedMessages;
    @Mock
    private HttpPost httpPost;
    @Mock
    private CloseableHttpResponse httpResponse;
    @Mock
    private CloseableHttpClient httpClient;
    @Mock
    private HttpClientBuilder httpClientBuilder;
    @InjectMocks
    private RecordChangedMessageHandler sut;

    @Before
    public void setup() {
        Map<String, String> headers = new HashMap<>();
        headers.put(DpsHeaders.ACCOUNT_ID, ACCOUNT_ID);
        headers.put(DpsHeaders.CORRELATION_ID, CORRELATION_ID);
        recordChangedMessages = RecordChangedMessages.builder()
                .attributes(headers).build();
        when(azureBootstrapConfig.getMaxDeliveryCount()).thenReturn("5");
    }

    @Test
    public void shouldThrow_whenHttpClientThrows() throws Exception{
        RuntimeException exp = new RuntimeException("httpClientBuilder build failed");
        when(httpClientBuilder.build()).thenThrow(exp);
        try {
            sut.sendMessagesToIndexer(recordChangedMessages, currentTry);
        }
        catch (Exception e) {
            verify(azureBootstrapConfig, times(1)).getMaxDeliveryCount();
            assertEquals("httpClientBuilder build failed", e.getMessage());
        }
    }

    @Test
    public void should_BackoffExponentially_whenHttpClientThrows_andCurrentTryLessThanMaxTry() throws Exception{
        RuntimeException exp = new RuntimeException("httpClientBuilder build failed");
        when(httpClientBuilder.build()).thenThrow(exp);
        java.sql.Timestamp before = null;
        long expectedWaitTime = ((long) Math.pow(2, currentTry) * 10L);

        try {
            before = new java.sql.Timestamp(new Date().getTime());
            sut.sendMessagesToIndexer(recordChangedMessages, currentTry);
        }
        catch (Exception e) {
            verify(azureBootstrapConfig, times(1)).getMaxDeliveryCount();
            java.sql.Timestamp after = new java.sql.Timestamp(new Date().getTime());
            long actualWaitTime = after.getTime() - before.getTime();
            assertTrue(actualWaitTime >= expectedWaitTime);
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
            sut.sendMessagesToIndexer(recordChangedMessages, maxTry);
        } catch (Exception e) {
            verify(azureBootstrapConfig, times(1)).getMaxDeliveryCount();
            java.sql.Timestamp after = new java.sql.Timestamp(new Date().getTime());
            long actualWaitTime = after.getTime() - before.getTime();
            assertTrue(actualWaitTime < backOffWaitTime);
        }
    }

    @Test
    public void shouldInvoke_httpPostMethod_whenHttpResponseCodeIsSuccess() throws Exception {
        when(httpClientBuilder.build()).thenReturn(httpClient);
        when(azureBootstrapConfig.getIndexerWorkerURL()).thenReturn(indexerWorkerUrl);
        whenNew(HttpPost.class).withArguments(indexerWorkerUrl).thenReturn(httpPost);
        doNothing().when(httpPost).setEntity(any(HttpEntity.class));
        doNothing().when(httpPost).setHeader(any());
        when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
        StatusLine status = new BasicStatusLine(new ProtocolVersion("http",1,1),200,"success");
        when(httpResponse.getStatusLine()).thenReturn(status);

        sut.sendMessagesToIndexer(recordChangedMessages, currentTry);

        verify(httpClient,times(1)).execute(httpPost);
        verify(azureBootstrapConfig, times(1)).getIndexerWorkerURL();
    }

    @Test
    public void shouldThrow_whenHttpPostMethod_ReturnsErrorReponseCode() throws Exception {
        when(httpClientBuilder.build()).thenReturn(httpClient);
        when(azureBootstrapConfig.getIndexerWorkerURL()).thenReturn(indexerWorkerUrl);
        whenNew(HttpPost.class).withArguments(indexerWorkerUrl).thenReturn(httpPost);
        doNothing().when(httpPost).setEntity(any(HttpEntity.class));
        doNothing().when(httpPost).setHeader(any());
        when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
        StatusLine status = new BasicStatusLine(new ProtocolVersion("http",1,1),400,"error");
        when(httpResponse.getStatusLine()).thenReturn(status);

        try {
            sut.sendMessagesToIndexer(recordChangedMessages, currentTry);
        } catch (Exception e) {
            verify(httpClient,times(1)).execute(httpPost);
            verify(azureBootstrapConfig, times(1)).getIndexerWorkerURL();
        }

    }

}
