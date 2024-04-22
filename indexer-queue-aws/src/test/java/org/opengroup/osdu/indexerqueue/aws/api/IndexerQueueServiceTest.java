/**
* Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*      http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.opengroup.osdu.indexerqueue.aws.api;


import org.springframework.test.util.ReflectionTestUtils;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class IndexerQueueServiceTest {


    @Mock
    private AmazonSQS sqsClient = mock(AmazonSQS.class);

    private static final int MAX_WAIT_TIME = 10;
    private static final int MAX_THREADS = 20;
    private static final int MAX_ALLOWED_MESSAGES = 25;
    private static final int MAX_BATCH_SIZE = 8;
    private static final String DEAD_LETTER_QUEUE_URL = "dead-letter-queue-url";
    private static final String QUEUE_URL = "storage-queue-url";
    private static final String TARGET_URL = "target-url";

    @Mock
    private EnvironmentVariables environmentVariables = Mockito.mock(EnvironmentVariables.class);

    private MockedConstruction<WorkerThread> workerThreadMockedConstruction;

    private MockedConstruction<MessageDeleter> deleterMockedConstruction;

    private MockedConstruction<MessageRetrier> retrierMockedConstruction;

    private MockedConstruction<MessageVisibilityModifier> visiblityMockedConstruction;

    private AmazonSQS sqsSupplier() {
        return sqsClient;
    }

    @Before
    public void setUp() {
        deleterMockedConstruction = Mockito.mockConstruction(MessageDeleter.class, (mock, context) -> {
            assertEquals(MAX_BATCH_SIZE, context.arguments().get(1));
            assertEquals(sqsClient, context.arguments().get(2));
        });
        retrierMockedConstruction = Mockito.mockConstruction(MessageRetrier.class, (mock, context) -> {
            assertEquals(MAX_BATCH_SIZE, context.arguments().get(1));
            assertEquals(sqsClient, context.arguments().get(2));
        });
        visiblityMockedConstruction = Mockito.mockConstruction(MessageVisibilityModifier.class, (mock, context) -> {
            assertEquals(MAX_BATCH_SIZE, context.arguments().get(1));
            assertEquals(sqsClient, context.arguments().get(2));
        });

        workerThreadMockedConstruction = Mockito.mockConstruction(WorkerThread.class, (mock, context) -> {
            assertEquals(environmentVariables, context.arguments().get(5));
        });

        when(environmentVariables.getDeadLetterQueueUrl()).thenReturn(DEAD_LETTER_QUEUE_URL);
        when(environmentVariables.getQueueUrl()).thenReturn(QUEUE_URL);
        when(environmentVariables.getMaxAllowedMessages()).thenReturn(MAX_ALLOWED_MESSAGES);
        when(environmentVariables.getTargetURL()).thenReturn(TARGET_URL);
        when(environmentVariables.getMaxWaitTime()).thenReturn(MAX_WAIT_TIME);
        when(environmentVariables.getMaxBatchRequestCount()).thenReturn(MAX_BATCH_SIZE);
        when(environmentVariables.getMaxIndexThreads()).thenReturn(MAX_THREADS);
    }

    @After
    public void tearDown() {
        workerThreadMockedConstruction.close();
        deleterMockedConstruction.close();
        retrierMockedConstruction.close();
        visiblityMockedConstruction.close();
    }

    @Test
    public void test_healthCheck_fails_whenWorkersExit() throws InterruptedException {
        // Arrange
        int numMessages = 10;
        List<Message> messages = new ArrayList<>();
        for (int i = 0; i < numMessages; i++) {
            Message msg = new Message();
            messages.add(msg);
        }

        IndexerQueueService service = new IndexerQueueService(environmentVariables, this::sqsSupplier);

        service.putMessages(messages);

        BlockingQueue incomingQueue = (BlockingQueue) ReflectionTestUtils.getField(service, "receivedMessages");
        assertEquals(numMessages, incomingQueue.size());

        assertEquals(1, retrierMockedConstruction.constructed().size());
        assertEquals(1, deleterMockedConstruction.constructed().size());
        assertEquals(1, visiblityMockedConstruction.constructed().size());
        assertEquals(MAX_THREADS, workerThreadMockedConstruction.constructed().size());
        assertTrue(service.isUnhealthy());
        assertEquals(2 * MAX_THREADS, workerThreadMockedConstruction.constructed().size());
        assertEquals(1, retrierMockedConstruction.constructed().size());
        assertEquals(1, deleterMockedConstruction.constructed().size());
        assertEquals(1, visiblityMockedConstruction.constructed().size());
    }
}
