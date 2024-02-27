/* Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@RunWith(MockitoJUnitRunner.class)
public class WorkerThreadTest {
    private static final int MAX_MESSAGES = 10;
    private final BlockingQueue<Message> incomingMessages = new ArrayBlockingQueue<>(MAX_MESSAGES);
    private final BlockingQueue<Message> deleteMessages = new ArrayBlockingQueue<>(MAX_MESSAGES);
    private final BlockingQueue<Message> retryMessages = new ArrayBlockingQueue<>(MAX_MESSAGES);
    private final BlockingQueue<Message> visibilityMessages = new ArrayBlockingQueue<>(MAX_MESSAGES);
    private final ExecutorService executorService = Executors.newFixedThreadPool(MAX_MESSAGES);

    private static final int MAX_WAIT_TIME = 1000;
    private static final String TARGET_URL = "someTargetURL";
    private static final String NEWINDEX_URL = String.format("%s/%s", TARGET_URL, WorkerThread.NEWINDEX_URL_PATH);
    private static final String REINDEX_URL = String.format("%s/%s", TARGET_URL, WorkerThread.REINDEX_URL_PATH);
    private static final String AUTHORIZED_TOKEN = "someAuthorizedToken";
    private static final String AUTHORIZED_KEY = "authorization";
    private static final String REINDEX_KEY = "ReIndexCursor";

    private void cancelHandler(Future<?> handlerFuture) throws InterruptedException {
        handlerFuture.cancel(true);
        // Should take a small amount of time to wait for the thread to loop.
        Thread.sleep(2 * MAX_WAIT_TIME);
        assertTrue(handlerFuture.isDone());
    }

    private WorkerThread getWorker() {
        return new WorkerThread(incomingMessages, retryMessages, deleteMessages, visibilityMessages, executorService, TARGET_URL, MAX_WAIT_TIME, MAX_WAIT_TIME);
    }

    @Test
    public void should_sendUnauthorizedMessages_to_retryQueue() throws InterruptedException {
        Message unauthorizedMessage = new Message();
        String unauthorizedId = "Unauthorized Message ID";
        unauthorizedMessage.setMessageId(unauthorizedId);
        WorkerThread testingImplementation = getWorker();
        Future<?> workerThreadExecutor = executorService.submit(testingImplementation);
        incomingMessages.add(unauthorizedMessage);

        Thread.sleep(MAX_WAIT_TIME);
        cancelHandler(workerThreadExecutor);

        assertEquals(0, incomingMessages.size());
        assertEquals(1, deleteMessages.size());
        assertEquals(1, retryMessages.size());
        assertEquals(0, visibilityMessages.size());

        Message receivedMessage = deleteMessages.take();
        assertEquals(unauthorizedId, receivedMessage.getMessageId());
        receivedMessage = retryMessages.take();
        assertEquals(unauthorizedId, receivedMessage.getMessageId());
    }

    private Message getAuthorizedMessage(String messageId) {
        return getAuthorizedMessage(messageId, null);
    }

    private Message getAuthorizedMessage(String messageId, String reIndexCursor) {
        Message authorizedMessage = new Message();
        authorizedMessage.setMessageId(messageId);
        Map<String, MessageAttributeValue> messageAttributes = authorizedMessage.getMessageAttributes();
        messageAttributes.put(AUTHORIZED_KEY, new MessageAttributeValue().withStringValue(AUTHORIZED_TOKEN));
        if (reIndexCursor != null) {
            messageAttributes.put(REINDEX_KEY, new MessageAttributeValue().withStringValue(reIndexCursor));
        }
        return authorizedMessage;
    }

    @Test
    public void should_processNewMessage_correctly() throws InterruptedException {
        String newIndexMessageId = "newIndexMessageId";
        Message message = getAuthorizedMessage(newIndexMessageId);
        Future<?> workerThreadExecutor = executorService.submit(() -> {
            try (MockedConstruction<NewIndexProcessor> newIndexProcessor = Mockito.mockConstruction(NewIndexProcessor.class, (mock, context) -> {
                when(mock.getResult()).thenReturn(CallableResult.PASS);
                when(mock.call()).thenReturn(mock);
            })) {
                WorkerThread testingImplementation = getWorker();
                testingImplementation.run();
            }
        });
        incomingMessages.add(message);

        Thread.sleep(MAX_WAIT_TIME);
        cancelHandler(workerThreadExecutor);

        assertEquals(0, incomingMessages.size());
        assertEquals(1, deleteMessages.size());
        assertEquals(0, retryMessages.size());
        assertEquals(0, visibilityMessages.size());

        Message receivedMessage = deleteMessages.take();
        assertEquals(AUTHORIZED_TOKEN, receivedMessage.getMessageAttributes().get(AUTHORIZED_KEY).getStringValue());
        assertNull(receivedMessage.getMessageAttributes().get(REINDEX_KEY));
        assertEquals(newIndexMessageId, receivedMessage.getMessageId());
    }

    @Test
    public void should_processReIndexMessage_correctly() throws Throwable {
        String reIndexMessageId = "reIndexMessageId";
        String reIndexCursor = "reIndexCursor";
        Message message = getAuthorizedMessage(reIndexMessageId, reIndexCursor);
        Future<?> workerThreadExecutor = executorService.submit(() -> {
            try (MockedConstruction<ReIndexProcessor> reIndexProcessor = Mockito.mockConstruction(ReIndexProcessor.class, (mock, context) -> {
                when(mock.getResult()).thenReturn(CallableResult.PASS);
                when(mock.call()).thenReturn(mock);
                assertEquals(REINDEX_URL, context.arguments().get(1));
            })) {
                WorkerThread testingImplementation = getWorker();
                testingImplementation.run();
            }
        });
        incomingMessages.add(message);
        Thread.sleep(MAX_WAIT_TIME);
        cancelHandler(workerThreadExecutor);

        assertEquals(0, incomingMessages.size());
        assertEquals(1, deleteMessages.size());
        assertEquals(0, retryMessages.size());
        assertEquals(0, visibilityMessages.size());

        Message receivedMessage = deleteMessages.take();
        assertEquals(AUTHORIZED_TOKEN, receivedMessage.getMessageAttributes().get(AUTHORIZED_KEY).getStringValue());
        assertEquals(reIndexCursor, receivedMessage.getMessageAttributes().get(REINDEX_KEY).getStringValue());
        assertEquals(reIndexMessageId, receivedMessage.getMessageId());
    }

    @Test
    public void should_processFailedMessage_correctly() throws InterruptedException {
        String failedIndexMessageId = "failedIndexMessageId";
        Message message = getAuthorizedMessage(failedIndexMessageId);
        try (MockedConstruction<NewIndexProcessor> newIndexProcessor = Mockito.mockConstruction(NewIndexProcessor.class, (mock, context) -> {
            Message messageArgument = ((Message)context.arguments().get(0));
            assertEquals(failedIndexMessageId, messageArgument.getMessageId());
            assertEquals(AUTHORIZED_TOKEN, messageArgument.getMessageAttributes().get(AUTHORIZED_KEY).getStringValue());
            assertNull(messageArgument.getMessageAttributes().get(REINDEX_KEY));
            assertEquals(NEWINDEX_URL, context.arguments().get(1));
            assertEquals(AUTHORIZED_TOKEN, context.arguments().get(2));
            when(mock.getResult()).thenReturn(CallableResult.FAIL);
            when(mock.call()).thenReturn(mock);
        })) {
            WorkerThread testingImplementation = getWorker();
            Future<?> workerThreadExecutor = executorService.submit(testingImplementation);
            incomingMessages.add(message);

            Thread.sleep(MAX_WAIT_TIME);
            cancelHandler(workerThreadExecutor);

            assertEquals(0, incomingMessages.size());
            assertEquals(0, deleteMessages.size());
            assertEquals(0, retryMessages.size());
            assertEquals(1, visibilityMessages.size());

            Message receivedMessage = visibilityMessages.take();
            assertEquals(failedIndexMessageId, receivedMessage.getMessageId());
        }
    }


    @Test
    public void should_processHungMessage_correctly() throws InterruptedException {
        String hungIndexMessageId = "hungIndexMessageId";
        Message message = getAuthorizedMessage(hungIndexMessageId);
        try (MockedConstruction<NewIndexProcessor> newIndexProcessor = Mockito.mockConstruction(NewIndexProcessor.class, (mock, context) -> {
            Message messageArgument = ((Message)context.arguments().get(0));
            assertEquals(hungIndexMessageId, messageArgument.getMessageId());
            assertEquals(AUTHORIZED_TOKEN, messageArgument.getMessageAttributes().get(AUTHORIZED_KEY).getStringValue());
            assertNull(messageArgument.getMessageAttributes().get(REINDEX_KEY));
            assertEquals(NEWINDEX_URL, context.arguments().get(1));
            assertEquals(AUTHORIZED_TOKEN, context.arguments().get(2));
            when(mock.getResult()).thenAnswer(new Answer<Object>() {
                @Override
                public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                    Thread.sleep(2 * MAX_WAIT_TIME);
                    return new Object();
                }
            });
        })) {
            WorkerThread testingImplementation = getWorker();
            Future<?> workerThreadExecutor = executorService.submit(testingImplementation);
            incomingMessages.add(message);

            Thread.sleep(MAX_WAIT_TIME);
            cancelHandler(workerThreadExecutor);

            assertEquals(0, incomingMessages.size());
            assertEquals(0, deleteMessages.size());
            assertEquals(0, retryMessages.size());
            assertEquals(1, visibilityMessages.size());

            Message receivedMessage = visibilityMessages.take();
            assertEquals(hungIndexMessageId, receivedMessage.getMessageId());
        }
    }
}
