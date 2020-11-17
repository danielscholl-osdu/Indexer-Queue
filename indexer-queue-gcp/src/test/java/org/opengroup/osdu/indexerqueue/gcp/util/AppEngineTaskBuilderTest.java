// Copyright 2017-2019, Schlumberger
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

package org.opengroup.osdu.indexerqueue.gcp.util;

import com.google.cloud.tasks.v2.CloudTasksClient;
import com.google.cloud.tasks.v2.Task;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opengroup.osdu.core.common.Constants;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.CloudTaskRequest;
import org.opengroup.osdu.core.gcp.util.HeadersInfo;
import org.opengroup.osdu.indexerqueue.gcp.config.IndexerQueueConfigurationPropertiesGcp;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CloudTasksClient.class})
public class AppEngineTaskBuilderTest {

    @Mock
    private IndexerQueueConfigurationPropertiesGcp configurationProperties;
    @Mock
    private javax.inject.Provider<IndexerQueueIdentifier> indexerQueueProvider;
    @Mock
    private IndexerQueueIdentifier indexerQueue;
    @Mock
    private DpsHeaders dpsHeaders;
    @Mock
    private HeadersInfo headersInfo;

    @Mock
    private CloudTasksClient client;

    @InjectMocks
    private AppEngineTaskBuilder sut;

    @Before
    public void setup() throws IOException {
        initMocks(this);
        mockStatic(CloudTasksClient.class);

        client = mock(CloudTasksClient.class);
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        when(this.headersInfo.getHeaders()).thenReturn(dpsHeaders);
        when(dpsHeaders.getHeaders()).thenReturn(headers);
        when(configurationProperties.getIndexerHost()).thenReturn("indexer-dot-test-ddl-us-services.appspot.com");
        when(configurationProperties.getGoogleCloudProject()).thenReturn("test-ddl-us-services");
        when(configurationProperties.getDeploymentLocation()).thenReturn("us-central1");
        when(this.indexerQueueProvider.get()).thenReturn(this.indexerQueue);
        when(this.indexerQueue.getQueueId()).thenReturn("test-queue");
        when(CloudTasksClient.create()).thenReturn(client);
    }

    @Test
    public void createTaskTest() throws IOException {
        Task expectedResponse = Task.newBuilder()
                .setName("projects/test-ddl-us-services/locations/us-central1/queues/common-indexer-queue/tasks/44173906993138094031").build();
        when(client.createTask(anyString(), any())).thenReturn(expectedResponse);

        CloudTaskRequest request = CloudTaskRequest.builder().url(Constants.WORKER_RELATIVE_URL).message("{}").build();

        Task response = this.sut.createTask(request);
        Assert.assertEquals(expectedResponse.getName(), response.getName());
    }
}