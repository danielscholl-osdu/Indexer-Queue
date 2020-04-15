// Copyright © Microsoft Corporation
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//      http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.indexerqueue.azure;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.TopicClient;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opengroup.osdu.indexerqueue.azure.ReEnqueueApplication;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.*;

@Ignore
@RunWith(PowerMockRunner.class)
@PrepareForTest({TopicClient.class})
public class ReEnqueueApplicationTest {

    private final String requestBodyValid = "{\"message\":\"{\\\"data\\\":[{\\\"id\\\":\\\"common:welldb:raj21\\\",\\\"kind\\\":\\\"common:welldb:wellbore:1.0.0\\\",\\\"op\\\":\\\"create\\\"}],\\\"account-id\\\":\\\"common\\\",\\\"data-partition-id\\\":\\\"common\\\",\\\"correlation-id\\\":\\\"ee85038e-4510-49d9-b2ec-3651315a4d00\\\"}\",\"url\":\"foo\"}";
    private final String requestBodyEmpty = "{}";
    private ExecutionContext executionContext;
    private Message message;

    @Mock
    private HttpRequestMessage request;
    @Mock
    private TopicClient topicClient;
//    @Mock
//    private ConnectionStringBuilder connectionStringBuilder;
    @Mock
    Response response;
    @Mock
    private Map<String, String> headers;
    @InjectMocks
    private ReEnqueueApplication sut;

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Before
    public void setup() throws Exception {
        environmentVariables.set("TOPIC_NAME", "topic-name");
        environmentVariables.set("SERVICE_BUS", "Endpoint=sb://resourcegroup.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=kms");
        executionContext = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(executionContext).getLogger();

//        mockStatic(TopicClient.class);
        ConnectionStringBuilder connectionStringBuilder = mock(ConnectionStringBuilder.class);
        whenNew(ConnectionStringBuilder.class).withAnyArguments().thenReturn(connectionStringBuilder);
        whenNew(TopicClient.class).withAnyArguments().thenReturn(topicClient);

        message = new Message(requestBodyValid);
        headers = new HashMap<>();
        headers.put("x-functions-key", "x-functions-key");
        when(request.getUri()).thenReturn(new URI("/"));
        when(request.getHttpMethod()).thenReturn(HttpMethod.POST);
    }

    @Test
    public void should_return200_when_request_isValid() throws ServiceBusException, InterruptedException {
        when(request.getHeaders()).thenReturn(headers);
        when(request.getBody()).thenReturn(requestBodyValid);
        HttpResponseMessage response = this.sut.run(request, executionContext);
        assertEquals(HttpStatus.SC_OK, response.getStatus());
    }

    @Test
    public void should_return200_when_request_isEmpty() throws ServiceBusException, InterruptedException {

        when(request.getBody()).thenReturn(requestBodyEmpty);
        Response response = (Response) this.sut.run(request, executionContext);
        assertEquals(HttpStatus.SC_OK, response.getStatus());
    }
}