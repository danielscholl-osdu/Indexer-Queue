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

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opengroup.osdu.core.common.Constants;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.CloudTaskRequest;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Queue.class, QueueFactory.class})
public class AppEngineTaskBuilderTest {

    @Mock
    private Queue queue;
    @Mock
    private IndexerQueueIdentifier indexerQueueProvider;
    @Mock
    private DpsHeaders dpsHeaders;
    @InjectMocks
    private AppEngineTaskBuilder sut;

    @Before
    public void setup() {
        initMocks(this);
        mockStatic(QueueFactory.class);

        Map<String, String> headers = new HashMap<>();
        when(this.dpsHeaders.getHeaders()).thenReturn(headers);
        when(this.indexerQueueProvider.getQueueId()).thenReturn("test-queue");
        when(QueueFactory.getQueue("test-queue")).thenReturn(queue);
    }

    @Test
    public void createTaskTest() {
        CloudTaskRequest request = CloudTaskRequest.builder().url(Constants.WORKER_RELATIVE_URL).message("{}").build();

        this.sut.createTask(request);
    }
}