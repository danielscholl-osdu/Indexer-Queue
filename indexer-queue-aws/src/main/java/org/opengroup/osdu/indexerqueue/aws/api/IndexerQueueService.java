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

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.*;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

public class IndexerQueueService implements AutoCloseable {

    private static final JaxRsDpsLog logger = LogProvider.getLogger();
    private final String targetURL;
    private final BlockingQueue<Message> receivedMessages;
    private final BlockingQueue<Message> deleteMessages;
    private final BlockingQueue<Message> changeVisibilityMessages;
    private final BlockingQueue<Message> retryMessages;
    private final ExecutorService primaryExecutor;
    private final ExecutorService workerExecutor;
    private final ExecutorService cleanupExecutor;
    private final Future<?> retryFuture;
    private final Future<?> deleteFuture;
    private final Future<?> visibilityFuture;
    private final List<Future<?>> workerFutures;
    public IndexerQueueService(EnvironmentVariables variables, Supplier<AmazonSQS> sqsSupplier) {
        int maxMessages = variables.getMaxAllowedMessages();
        int maxThreads = variables.getMaxIndexThreads();
        int maxBatchThreads = variables.getMaxBatchRequestCount();
        targetURL = variables.getTargetURL();
        receivedMessages = new ArrayBlockingQueue<>(maxMessages * 2);
        deleteMessages = new ArrayBlockingQueue<>(maxMessages);
        retryMessages = new ArrayBlockingQueue<>(maxMessages);
        changeVisibilityMessages = new ArrayBlockingQueue<>(maxMessages);
        primaryExecutor = Executors.newFixedThreadPool(maxThreads);
        workerExecutor = Executors.newFixedThreadPool(maxThreads);
        cleanupExecutor = Executors.newFixedThreadPool(3);
        retryFuture = cleanupExecutor.submit(new MessageRetrier(retryMessages, maxBatchThreads, sqsSupplier.get(), variables.getDeadLetterQueueUrl()));
        deleteFuture = cleanupExecutor.submit(new MessageDeleter(deleteMessages, maxBatchThreads, sqsSupplier.get(), variables.getQueueUrl()));
        visibilityFuture = cleanupExecutor.submit(new MessageVisibilityModifier(changeVisibilityMessages, maxBatchThreads, sqsSupplier.get(), variables.getQueueUrl()));
        workerFutures = new ArrayList<>();
        for (int i = 0; i < maxThreads; ++i) {
            workerFutures.add(primaryExecutor.submit(generateNewWorker()));
        }
    }

    private WorkerThread generateNewWorker() {
        return new WorkerThread(receivedMessages, retryMessages, deleteMessages, changeVisibilityMessages, workerExecutor, targetURL);
    }

    public int getNumMessages() {
        return receivedMessages.size();
    }

    public void putMessages(List<Message> messages) {
        receivedMessages.addAll(messages);
    }

    private boolean isWorkerDone(Future<?> future, String message) throws InterruptedException {
        boolean retVal = future.isDone();
        if (retVal) {
            boolean hasException = false;
            try {
                future.get(100, TimeUnit.MILLISECONDS);
            } catch (ExecutionException | TimeoutException e) {
                logger.error(message, e);
                hasException = true;
            }
            if (!hasException) {
                logger.error(String.format("%s finished with no reason.", message));
            }
        }
        return retVal;
    }

    public boolean isUnhealthy() throws InterruptedException {
        int numFailedWorkers = 0;
        for (int i = 0; i < workerFutures.size(); ++i) {
            if (isWorkerDone(workerFutures.get(i), String.format("Worker thread %d is done", i))) {
                workerFutures.set(i, primaryExecutor.submit(generateNewWorker()));
                ++numFailedWorkers;
            }
        }
        if (numFailedWorkers > 0) {
            logger.error(String.format("There were %d failed workers that had to be restarted.", numFailedWorkers));
        }
        boolean retryDone = isWorkerDone(retryFuture, "Retry thread is done");
        boolean deleteDone = isWorkerDone(deleteFuture, "Delete thread is done");
        boolean visibilityDone = isWorkerDone(visibilityFuture, "Visibility thread is done");
        return retryDone || deleteDone || visibilityDone;
    }

    @Override
    public void close() {
        workerExecutor.shutdownNow();
        primaryExecutor.shutdownNow();
        cleanupExecutor.shutdownNow();
    }
}
